/*
  Copyright [2021] [Manycore Tech Inc.]

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.qunhe.avocado;

import com.qunhe.avocado.lib.constant.Settings;
import com.qunhe.avocado.lib.global.Global;
import com.qunhe.avocado.lib.global.Platform;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author CPPAlien
 */
public class RunCukesTest {
    private static final String[] defaultOptions = {
        "--glue", "com.qunhe.avocado",
        "--plugin", "pretty",
        "--plugin", "json:target/cucumber.json",
        "--plugin", "html:target/cucumber",
    };

    public static void main(String[] args) {
        Global.VARIABLES.clear();
        boolean isSetLogDir = false;
        List<String> cucumberArgs = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i]);
            switch (args[i]) {
                case "--udid":
                    if (i + 1 == args.length || args[i + 1].startsWith("--")) break;
                    Platform.UDID = args[i + 1];
                    i++;
                    break;
                case "--server":
                    if (i + 1 == args.length || args[i + 1].startsWith("--")) break;
                    Platform.SERVER_ADDRESS = args[i + 1];
                    i++;
                    break;
                case "--script":
                    if (i + 1 == args.length || args[i + 1].startsWith("--")) break;
                    Platform.SCRIPT_LOCATION = args[i + 1];
                    i++;
                    break;
                case "--platform":
                    if (i + 1 == args.length || args[i + 1].startsWith("--")) break;
                    Platform.CURRENT_PLATFORM = args[i + 1];
                    i++;
                    break;
                case "--app":
                    if (i + 1 == args.length || args[i + 1].startsWith("--")) break;
                    Platform.PACKAGE_LOCATION = args[i + 1];
                    i++;
                    break;
                case "--chromeDriver":
                    if (i + 1 == args.length || args[i + 1].startsWith("--")) break;
                    Platform.CHROME_DRIVER_PATH = args[i + 1];
                    i++;
                    break;
                case "--env":
                    if (i + 1 == args.length || args[i + 1].startsWith("--")) break;
                    String[] envs = args[i + 1].split("\n");
                    for (String env : envs) {
                        Matcher matcher = Pattern.compile("^(?:([^\\s:]+?):)\\s*(.+)$").matcher(env.trim());
                        if (matcher.matches()) {
                            Global.VARIABLES.put(matcher.group(1), matcher.group(2));
                        }
                    }
                    i++;
                    break;
                case "--logDir":
                    if (i + 1 == args.length || args[i + 1].startsWith("--")) break;
                    for (String option : defaultOptions) {
                        cucumberArgs.add(option.replace("target", args[i + 1]));
                    }
                    Settings.TARGET_DIR = Settings.TARGET_DIR.replace("target", args[i + 1]);
                    isSetLogDir = true;
                    i++;
                    break;
                case "--remote-debugging-port":
                    if (i + 1 == args.length || args[i + 1].startsWith("--")) break;
                    Platform.CHROME_DEBUGGING = args[i] + "=" + args[i + 1];
                    i++;
                    break;
                case "--try":
                    if (i + 1 == args.length || args[i + 1].startsWith("--")) break;
                    Settings.FAIL_TRY_TIMES = Integer.parseInt(args[i + 1]);
                    i++;
                    break;
                case "--showProcess":
                    Platform.SHOW_PROCESS = true;
                    break;
                case "--name":
                case "--tags":
                    if (i + 1 == args.length || args[i + 1].startsWith("--")) break;
                    cucumberArgs.add(args[i]);
                    cucumberArgs.add(args[i + 1]);
                    i++;
                    break;
                default:
                    cucumberArgs.add(args[i]);
                    break;
            }
        }
        if (Platform.SCRIPT_LOCATION == null) {
            Platform.SCRIPT_LOCATION = System.getProperty("user.dir");
        }
        // 加载变量，设备全局变量优先级高于脚本变量
        loadVariables();
        if (isSetLogDir) {
            cucumber.api.cli.Main.main(cucumberArgs.toArray(new String[0]));
        } else {
            Stream<String> cucumberOptions = Stream.concat(Stream.of(defaultOptions), cucumberArgs.stream());
            cucumber.api.cli.Main.main(cucumberOptions.toArray(String[]::new));
        }
    }

    // 加载变量，设备全局变量优先级高于脚本变量
    public static void loadVariables() {
        try {
            Yaml yaml = new Yaml();
            String localPath = Paths.get(Platform.SCRIPT_LOCATION, Settings.NEW_ENV_FILE).toString();
            Map<String, Object> scriptVariables = yaml.load(new FileInputStream(localPath));
            for (String key : scriptVariables.keySet()) {
                Global.VARIABLES.putIfAbsent(key, scriptVariables.get(key));
            }
        } catch (FileNotFoundException e) {
            // ignore
        }

        // 注入系统保留变量
        Global.VARIABLES.put("SYSTEM_TIMESTAMP", String.valueOf(new Date().getTime()));
        Calendar c = Calendar.getInstance();
        Global.VARIABLES.put("SYSTEM_YEAR", String.valueOf(c.get(Calendar.YEAR)));
        Global.VARIABLES.put("SYSTEM_MONTH", String.valueOf(c.get(Calendar.MONTH) + 1));
        Global.VARIABLES.put("SYSTEM_DAY", String.valueOf(c.get(Calendar.DATE)));
        Global.VARIABLES.put("SYSTEM_HOUR", String.valueOf(c.get(Calendar.HOUR_OF_DAY)));
        Global.VARIABLES.put("SYSTEM_MINUTE", String.valueOf(c.get(Calendar.MINUTE)));
        Global.VARIABLES.put("SYSTEM_SECOND", String.valueOf(c.get(Calendar.SECOND)));
    }
}
