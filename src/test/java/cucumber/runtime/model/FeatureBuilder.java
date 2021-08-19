package cucumber.runtime.model;

import com.qunhe.avocado.lib.constant.TagNames;
import com.qunhe.avocado.lib.global.Global;
import cucumber.runtime.CucumberException;
import cucumber.runtime.io.Resource;
import cucumber.util.Encoding;
import gherkin.AstBuilder;
import gherkin.Parser;
import gherkin.ParserException;
import gherkin.TokenMatcher;
import gherkin.ast.GherkinDocument;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeatureBuilder {
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private final List<CucumberFeature> cucumberFeatures;
    private final char fileSeparatorChar;
    private final MessageDigest md5;
    private final Map<String, String> pathsByChecksum = new HashMap<String, String>();

    public FeatureBuilder(List<CucumberFeature> cucumberFeatures) {
        this(cucumberFeatures, File.separatorChar);
    }

    FeatureBuilder(List<CucumberFeature> cucumberFeatures, char fileSeparatorChar) {
        this.cucumberFeatures = cucumberFeatures;
        this.fileSeparatorChar = fileSeparatorChar;
        try {
            this.md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new CucumberException(e);
        }
    }

    String featureViewport = null;
    private boolean parseLines(Path currentPath, String line, List<String> afterLines) throws IOException {
        final String REGEX = "^\\*\\s+macro\\s+([/.\\w\u4e00-\u9fa5]+)(.*)$";
        Matcher matcher = Pattern.compile(REGEX).matcher(line.trim());
        if (matcher.matches()) {
            Path macro = currentPath.getParent()
                .resolve(String.format("%s.macro", matcher.group(1))).normalize();
            File file = macro.toFile();
            if (!file.exists()) {
                System.out.printf("%s is not found%n", macro.toString());
                throw new CucumberException(String.format("%s is not found", macro.toString()));
            }
            String macroContent = FileUtils.readFileToString(file, "UTF-8");
            // 替换入参
            String params = matcher.group(2);
            Matcher paramsMather = Pattern.compile("(\\s+\".+?\")|(\\s+-?[\\d.]+)").matcher(params);
            int index = 1;
            while (paramsMather.find()) {
                String content = paramsMather.group().trim();
                if (content.startsWith("\"") && content.endsWith("\"")) {
                    content = content.substring(1, content.length() - 1);
                }
                macroContent = macroContent.replace(String.format("${%d}", index), content);
                index++;
            }
            macroContent = macroContent.replaceAll("\\$\\{\\d+}", "");
            String[] macroLines = macroContent.split("\n");
            for (String macroLine : macroLines) {
                if (Pattern.matches(REGEX, macroLine)) {
                    boolean result = parseLines(currentPath, macroLine, afterLines);
                    if (result) {
                        return true;
                    }
                } else if (!macroLine.trim().startsWith("@")) {
                    afterLines.add(macroLine);
                }
                if (macroLine.trim().startsWith(TagNames.VIEWPORT) && featureViewport != null) {
                    // 如果 feature 设置了 viewport 并且 macro 也设置了，如果不一致则直接报错
                    if (!StringUtils.equals(featureViewport, macroLine.trim())) {
                        throw new RuntimeException(
                            String.format("macro %s viewport is not compatible", file.getName()));
                    }
                }
            }
        } else {
            afterLines.add(line);
            if (line.trim().startsWith(TagNames.VIEWPORT)) {
                featureViewport = line.trim();
            }
        }
        return false;
    }

    public void parse(Resource resource) throws IOException {
        Global.DEBUGGER_SCENARIOS.clear();
        String gherkin = read(resource);
        // dq add
        Path currentPath = Paths.get(resource.getAbsolutePath());
        String[] lines = gherkin.split("\n");
        List<String> afterLines = new ArrayList<>();

        for (String line : lines) {
            if (parseLines(currentPath, line, afterLines)) {
                break;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        boolean ignore = false;
        String currentScenarioName = "";
        for (String line : afterLines) {
            if (Pattern.matches("^(\\s*)debugger(\\s*)$", line)) {
                Global.DEBUGGER_SCENARIOS.put(currentScenarioName, true);
                ignore = true;
            }
            if (Pattern.matches("^Scenario:.*$", line.trim())) {
                currentScenarioName = line.trim().substring("Scenario:".length()).trim();
                ignore = false;
            }
            if (ignore) {
                continue;
            }

            stringBuilder.append(line);
            stringBuilder.append("\n");
        }
        gherkin = stringBuilder.toString();
        // dq add
        String checksum = checksum(gherkin);
        String path = pathsByChecksum.get(checksum);
        if (path != null) {
            return;
        }
        pathsByChecksum.put(checksum, resource.getPath());

        Parser<GherkinDocument> parser = new Parser<>(new AstBuilder());
        TokenMatcher matcher = new TokenMatcher();
        try {
            GherkinDocument gherkinDocument = parser.parse(gherkin, matcher);
            CucumberFeature feature = new CucumberFeature(gherkinDocument, convertFileSeparatorToForwardSlash(resource.getPath()), gherkin);
            cucumberFeatures.add(feature);
        } catch (ParserException e) {
            throw new CucumberException(e);
        }
     }

    private String convertFileSeparatorToForwardSlash(String path) {
        return path.replace(fileSeparatorChar, '/');
    }

    private String checksum(String gherkin) {
        return new BigInteger(1, md5.digest(gherkin.getBytes(UTF8))).toString(16);
    }

    public String read(Resource resource) {
        try {
            String source = Encoding.readFile(resource);
            return source;
        } catch (IOException e) {
            throw new CucumberException("Failed to read resource:" + resource.getPath(), e);
        }
    }
}
