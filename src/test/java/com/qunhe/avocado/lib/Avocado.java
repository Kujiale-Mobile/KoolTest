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
package com.qunhe.avocado.lib;

import com.google.gson.Gson;
import com.qunhe.avocado.lib.annotations.Mobile_Only;
import com.qunhe.avocado.lib.annotations.Web_Only;
import com.qunhe.avocado.lib.constant.*;
import com.qunhe.avocado.lib.global.Global;
import com.qunhe.avocado.lib.global.Platform;
import com.qunhe.avocado.lib.model.Device;
import com.qunhe.avocado.lib.model.Mouse;
import com.qunhe.avocado.lib.model.MatchRect;
import com.qunhe.avocado.lib.util.ElementUtil;
import com.qunhe.avocado.lib.util.Log;
import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.runtime.CucumberException;
import io.appium.java_client.MobileElement;
import io.appium.java_client.Setting;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.yaml.snakeyaml.Yaml;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author CPPAlien
 */
public class Avocado {
    private static volatile Avocado mAvocado;
    private static Scenario mScenario;
    private final Set<String> mTagNames = new HashSet<>();
    private final Mouse currentMouse = new Mouse();

    public static Avocado get() {
        if (mAvocado == null) {
            synchronized (Avocado.class) {
                if (mAvocado == null) {
                    mAvocado = new Avocado();
                }
            }
        }
        return mAvocado;
    }

    public void setCurrentMouse(int x, int y) {
        currentMouse.setX(x);
        currentMouse.setY(y);
    }

    public Mouse getCurrentMouse() {
        return currentMouse;
    }

    /**
     * 根据 selector 来查找元素
     *
     * @param arg 以 element:// 开头的元素查找
     * @return WebElement 列表，无的话为 empty
     */
    public List<WebElement> findElementsBySelector(String arg) {
        if (!arg.startsWith(Schemes.ELEMENT)) {
            throw new RuntimeException(arg + " is illegal when findElementsBySelector");
        }
        String name = arg.substring(Schemes.ELEMENT.length());
        final String CSS_SELECTOR_PRE = "cssSelector/";
        final String CLASSNAME_PRE = "className/";
        final String ID_PRE = "id/";
        final String XPATH_PRE = "xpath/";
        final String AID_PRE = "aId/";
        String cssSelector = null;
        String xpath = null;
        String id = null;
        String className = null;
        @Mobile_Only
        String aId = null;

        if (name.startsWith(CSS_SELECTOR_PRE)) {
            cssSelector = name.substring(CSS_SELECTOR_PRE.length());
        } else if (name.startsWith(XPATH_PRE)) {
            xpath = name.substring(XPATH_PRE.length());
        } else if (name.startsWith(ID_PRE)) {
            id = name.substring(ID_PRE.length());
        } else if (name.startsWith(AID_PRE)) {
            aId = name.substring(AID_PRE.length());
        } else if (name.startsWith(CLASSNAME_PRE)) {
            className = name.substring(CLASSNAME_PRE.length());
        } else {
            cssSelector = ElementUtil.instance().get(name).getElementCssSelector();
            xpath = ElementUtil.instance().get(name).getElementXPath();
            id = ElementUtil.instance().get(name).getElementId();
            className = ElementUtil.instance().get(name).getElementClassName();
            aId = ElementUtil.instance().get(name).getAccessibilityId();
        }
        if (StringUtils.isNotEmpty(aId)) {
            aId = replaceVariables(aId);
            List<MobileElement> elements = DriverManager.get().mobileDriver().findElementsByAccessibilityId(aId);
            if (elements.size() > 0) {
                return new ArrayList<>(elements);
            }
        }

        List<WebElement> elements = new ArrayList<>();
        if (!StringUtils.isEmpty(className)) {
            className = replaceVariables(className);
            String[] tempClassNames = className.trim().split("\\s+");
            List<WebElement> baseElements = findWebElements(By.className(tempClassNames[0]));
            if (tempClassNames.length == 1 && baseElements.size() > 0) {
                elements.addAll(baseElements);
            } else if (baseElements.size() > 0) {
                // 取交集
                List<List<WebElement>> tempElementsArray = new ArrayList<>();
                for (int i = 1; i < tempClassNames.length; i++) {
                    tempElementsArray.add(findWebElements(By.className(tempClassNames[i])));
                }
                elements = baseElements.stream().filter(webElement -> {
                    int containsCount = 0;
                    for (List<WebElement> tempElements : tempElementsArray) {
                        for (WebElement tempElement : tempElements) {
                            if (webElement.equals(tempElement)) {
                                containsCount++;
                                break;
                            }
                        }
                    }
                    return containsCount >= tempElementsArray.size();
                }).collect(Collectors.toList());
            }
        }
        if (!StringUtils.isEmpty(id) && elements.isEmpty()) {
            id = replaceVariables(id);
            By by = By.id(id);
            elements.addAll(Platform.isMobile() ?
                findMobileElements(by) : findWebElements(by));
        }
        if (!StringUtils.isEmpty(cssSelector) && elements.isEmpty()) {
            cssSelector = replaceVariables(cssSelector);
            By by = By.cssSelector(cssSelector);
            elements.addAll(Platform.isMobile() ?
                findMobileElements(by) : findWebElements(by));
        }
        if (!StringUtils.isEmpty(xpath) && elements.isEmpty()) {
            xpath = replaceVariables(xpath);
            By by = By.xpath(xpath);
            elements.addAll(Platform.isMobile() ?
                findMobileElements(by) : findWebElements(by));
        }

        return elements;
    }

    /**
     * element:// 方式的查找
     *
     * @param arg 元素名，比如 element://author
     * @return List<WebElement>
     * @throws NotFoundException 未找到
     */
    public List<WebElement> findElement(String arg, @NotNull Long realTimeOut) throws NotFoundException {
        long startTime = System.currentTimeMillis();
        while (true) {
            sleep(Duration.ofMillis(500));
            List<WebElement> elements = findElementsBySelector(arg);

            if (elements.size() > 0) {
                return elements;
            }

            if (System.currentTimeMillis() - startTime > realTimeOut) {
                throw new NotFoundException();
            }
        }
    }

    public void closeApp() {
        DriverManager.get().mobileDriver().closeApp();
    }

    public List<MatchRect.Point> findElementCenter(String arg) {
        return findElementCenter(arg, Settings.TIMEOUT);
    }

    /**
     * 找某个元素的中点坐标
     *
     * @param arg 输入内容
     * @return 中点坐标x，y
     * @throws NotFoundException 未找到
     */
    public List<MatchRect.Point> findElementCenter(String arg, Long timeout) {
        List<MatchRect.Point> points = new ArrayList<>();
        if (arg.startsWith(Schemes.ELEMENT)) {
            List<WebElement> elements = Avocado.get().findElement(arg, timeout);
            for (WebElement element : elements) {
                Point point = element.getRect().getPoint();
                points.add(new MatchRect.Point(point.x, point.y));
            }
        } else {
            List<MatchRect> matchRects = findRectsByName(arg, timeout);
            if (matchRects != null) {
                for (MatchRect rect : matchRects) {
                    points.add(rect.center());
                }
            }
        }
        if (points.isEmpty()) {
            throw new NotFoundException();
        } else {
            return points;
        }
    }

    @Web_Only
    public boolean findTitleExists(String name, int timeout) {
        long startTime = System.currentTimeMillis();
        long realTimeOut = timeout < 0 ? Settings.TIMEOUT : timeout * 1000L;
        name = replaceVariables(name);
        while (true) {
            sleep(Duration.ofMillis(500));
            String title = DriverManager.get().baseDriver().getTitle();
            boolean isExist = title != null && title.contains(name);
            if (isExist) {
                return true;
            }
            if (System.currentTimeMillis() - startTime > realTimeOut) {
                Assert.assertEquals(name, title);
                return false;
            }
        }
    }

    /**
     * 根据文本查找元素
     *
     * @param arg         文本信息，可以用 | 分割，并可以用 * * 包裹表示 contains
     * @param useContains 是否做包含查找
     * @return WebElement 列表
     */
    public List<WebElement> findElementsByText(String arg, boolean useContains) {
        arg = replaceVariables(arg);
        String[] names = arg.split("\\|");
        List<WebElement> list = new ArrayList<>();
        for (String name : names) {
            // 如果内容左右各有一个星好，则使用包含方式查找
            boolean contains = false;
            if (name.startsWith("*") && name.endsWith("*")) {
                contains = true;
                name = name.substring(1, name.length() - 1);
            }
            if (useContains) {
                contains = true;
            }
            String xpath;
            if (Platform.ANDROID.equals(Platform.CURRENT_PLATFORM)) {
                if (contains) {
                    xpath = "//*[contains(@text, '" + name + "')]";
                } else {
                    xpath = "//*[@text='" + name + "']";
                }
                list.addAll(DriverManager.get().baseDriver().findElements(By.xpath(xpath)));
                if (list.size() == 0) {
                    if (contains) {
                        xpath = ".//*[contains(@content-desc, '" + name + "')]";
                    } else {
                        xpath = ".//*[@content-desc='" + name + "']";
                    }
                    list.addAll(DriverManager.get().baseDriver().findElements(By.xpath(xpath)));
                }
            } else if (Platform.IOS.equals(Platform.CURRENT_PLATFORM)) {
                if (contains) {
                    xpath = "//*[contains(@name, '" + name + "')] | //*[contains(@value, '" + name + "')]";
                    list.addAll(DriverManager.get().baseDriver().findElements(By.xpath(xpath)));
                } else {
                    Log.i("start find " + name);
                    // 由于 iOS 在 xpath 查找上有些慢，优化一下
                    List<MobileElement> mobileElements = DriverManager.get().mobileDriver().findElementsByAccessibilityId(name);
                    if (mobileElements.size() > 0) {
                        Log.i("find by aid " + name);
                        list.addAll(mobileElements);
                    } else {
                        xpath = "//*[@name='" + name + "'] | //*[@value='" + name + "']";
                        list.addAll(DriverManager.get().baseDriver().findElements(By.xpath(xpath)));
                    }
                }
            } else {
                if (contains) {
                    xpath = XPath.WEB_ELEMENT_CONTAIN_NAME.replaceAll("%s", name);
                } else {
                    xpath = XPath.WEB_ELEMENT_EQUAL_NAME.replaceAll("%s", name);
                }
                List<WebElement> webElements = findWebElements(By.xpath(xpath));
                list.addAll(webElements);
            }
        }
        return list;
    }

    /**
     * 通过名称，组装 xpath 来查找 元素
     *
     * @param arg     名称
     * @param timeout 超时时间
     * @return WebElement
     */
    public List<WebElement> findElementByName(String arg, boolean useContains, Long timeout) {
        long startTime = System.currentTimeMillis();

        boolean isAll = false;
        if (Platform.CURRENT_PLATFORM.equals(Platform.IOS)) {
            isAll = true;
        }
        while (true) {
            sleep(Duration.ofMillis(500));
            List<WebElement> elements = new ArrayList<>();
            List<WebElement> list = findElementsByText(arg, useContains);
            for (WebElement element : list) {
                // 由于 ios 会存在用 button 覆盖一些文字，而导致文字变成 visible = false 的情况，所以暂且先不要忽略未显示的情况，先观察一下
                if (element.isDisplayed() || isAll) {
                    elements.add(element);
                }
            }
            if (elements.size() > 0) {
                return elements;
            }
            if (System.currentTimeMillis() - startTime > timeout) {
                throw new NotFoundException();
            }
        }
    }

    public List<WebElement> findWebElements(By by) {
        return findWebElements(by, true);
    }

    public List<MobileElement> findMobileElements(By by) {
        List<MobileElement> list = new ArrayList<>();
        try {
            MobileElement mobileElement = DriverManager.get().mobileDriver().findElement(by);
            list.add(mobileElement);
        } catch (Exception e) {
            // ignore
        }
        return list;
    }

    /**
     * 查找 Web 元素，递归 iframe 查找
     * 并筛选只有 display 的元素
     */
    public List<WebElement> findWebElements(By by, boolean onlyDisplay) {
        List<WebElement> displayedElements = new ArrayList<>();
        List<WebElement> webElements = DriverManager.get().baseDriver().findElements(by);
        for (WebElement element : webElements) {
            if (element.isDisplayed() || !onlyDisplay || "input".equals(element.getTagName())) {
                displayedElements.add(element);
            }
        }
        if (displayedElements.isEmpty()) {
            List<Integer> iframeIndexList = new ArrayList<>();
            List<WebElement> elements = DriverManager.get().baseDriver().findElements(By.tagName("iframe"));
            for (int i = 0; i < elements.size(); ++i) {
                if (elements.get(i).isDisplayed()) {
                    iframeIndexList.add(i);
                }
            }
            for (int index : iframeIndexList) {
                DriverManager.get().baseDriver().switchTo().frame(index);
                webElements = DriverManager.get().baseDriver().findElements(by);
                for (WebElement element : webElements) {
                    if (element.isDisplayed() || !onlyDisplay) {
                        displayedElements.add(element);
                    }
                }
                if (displayedElements.isEmpty()) {
                    DriverManager.get().baseDriver().switchTo().defaultContent();
                } else {
                    break;
                }
            }
        }

        return displayedElements;
    }

    private List<WebElement> findInputElements(String predicates) {
        List<WebElement> list = new ArrayList<>();
        final String ANDROID_XPATH = "//android.widget.EditText<Predicates>";
        final String IOS_XPATH = "//XCUIElementTypeTextField<Predicates> | //XCUIElementTypeTextView<Predicates> | //XCUIElementTypeSecureTextField<Predicates>";
        final String WEB_XPATH = "//input<Predicates> | //textarea<Predicates>";
        if (Platform.ANDROID.equals(Platform.CURRENT_PLATFORM)) {
            list.addAll(DriverManager.get().baseDriver()
                .findElementsByXPath(ANDROID_XPATH.replaceAll("<Predicates>", predicates)));
        } else if (Platform.IOS.equals(Platform.CURRENT_PLATFORM)) {
            list.addAll(DriverManager.get().baseDriver()
                .findElementsByXPath(IOS_XPATH.replaceAll("<Predicates>", predicates)));
        } else {
            List<WebElement> webElements =
                findWebElements(By.xpath(WEB_XPATH.replaceAll("<Predicates>", predicates)));
            list.addAll(webElements);
        }
        return list;
    }

    /**
     * 通过名称，组装 xpath 来查找 元素
     *
     * @param arg     名称
     * @param timeout 超时时间
     * @return WebElement
     */
    public List<WebElement> findInputElementsByName(String arg, Long timeout) {
        long startTime = System.currentTimeMillis();
        arg = replaceVariables(arg);
        String[] names = arg.split("\\|");

        while (true) {
            sleep(Duration.ofMillis(500));
            List<WebElement> elements = new ArrayList<>();
            List<WebElement> list = new ArrayList<>();
            if (StringUtils.isNotEmpty(arg)) {
                for (String name : names) {
                    if (Platform.ANDROID.equals(Platform.CURRENT_PLATFORM)) {
                        list.addAll(findInputElements("[contains(@text,'" + name + "')]"));
                    } else if (Platform.IOS.equals(Platform.CURRENT_PLATFORM)) {
                        list.addAll(findInputElements("[contains(@value,'" + name + "')]"));
                    } else {
                        list.addAll(findInputElements("[contains(@placeholder,'" + name + "')]"));
                    }
                }
            } else {
                list.addAll(findInputElements(""));
            }
            for (WebElement element : list) {
                if (element.isDisplayed()) {
                    elements.add(element);
                }
            }
            if (elements.size() > 0) {
                return elements;
            }
            if (System.currentTimeMillis() - startTime > timeout) {
                return elements;
            }
        }
    }

    /**
     * 根据名称查找该文字所在位置信息
     *
     * @param arg 名称
     */
    private List<MatchRect> findRectsByName(String arg, Long timeout) {
        arg = replaceVariables(arg);
        List<MatchRect> matchRects = new ArrayList<>();
        List<WebElement> elements = findElementByName(arg, false, timeout);
        if (elements != null) {
            for (WebElement element : elements) {
                sleep(Duration.ofMillis(500));
                Rectangle rect = element.getRect();
                matchRects.add(new MatchRect(rect.getX(), rect.getY(),
                    rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight()));
            }
        }
        if (matchRects.isEmpty()) {
            return null;
        } else {
            return matchRects;
        }
    }

    @Mobile_Only
    public void swipe(int startX, int startY, int endX, int endY) {
        // 边界点击会报错，检测到是边界时，向中间挪一下
        if (startX <= 0) startX = 1;
        if (startY <= 0) startY = 1;
        if (endX <= 0) endX = 1;
        if (endY <= 0) endY = 1;
        if (startX >= Device.screenWidth) startX = Device.screenWidth - 1;
        if (startY >= Device.screenHeight) startY = Device.screenHeight - 1;
        if (endX >= Device.screenWidth) endX = Device.screenWidth - 1;
        if (endY >= Device.screenHeight) endY = Device.screenHeight - 1;
        new TouchAction(DriverManager.get().mobileDriver()).press(PointOption.point(startX, startY))
            .waitAction(WaitOptions.waitOptions(Duration.ofSeconds(1)))
            .moveTo(PointOption.point(endX, endY)).release().perform();
    }

    public void autoAcceptAlertIfExists(String type) {
        WebDriverWait wait = new WebDriverWait(DriverManager.get().baseDriver(), 5);
        wait.until(ExpectedConditions.alertIsPresent());
        if (type.equals("accept")) {
            DriverManager.get().baseDriver().switchTo().alert().accept();
        } else {
            DriverManager.get().baseDriver().switchTo().alert().dismiss();
        }
    }

    /**
     * 打开某个页面，支持使用 ${} 进行环境变量配置
     *
     * @param url 跳转的链接
     */
    public void open(String url) {
        DriverManager.get().baseDriver().get(replaceVariables(url));
    }

    public void sleep(Duration duration) {
        try {
            Sleeper.SYSTEM_SLEEPER.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 替换内容中的变量（${}）部分
     *
     * @param string 值
     * @return 替换后的内容
     */
    public String replaceVariables(String string) {
        if (Global.VARIABLES.isEmpty()) {
            return string;
        }
        Matcher matcher = Pattern.compile("\\$\\{([^$]+)}").matcher(string);
        String afterString = string;
        while (matcher.find()) {
            String content = matcher.group(1);
            Object object;
            if (content.indexOf(".") > 0) {
                // 有 . 需要解析数据
                object = Global.VARIABLES.get(content.substring(0, content.indexOf(".")));
            } else {
                object = Global.VARIABLES.get(content);
            }
            if (object == null) {
                throw new CucumberException(String.format("%s is not found", content));
            }
            if (!(object instanceof String)) {
                Matcher keyMatcher = Pattern.compile("\\.([^$.]+)").matcher(content);
                while (keyMatcher.find()) {
                    String key = keyMatcher.group(1);
                    if (object instanceof Map) {
                        object = ((Map) object).get(key);
                    } else if (object instanceof ArrayList) {
                        object = ((ArrayList) object).get(Integer.parseInt(key));
                    } else {
                        break;
                    }
                }
            }
            String replaceString;
            if (object instanceof String) {
                replaceString = object.toString();
            } else {
                replaceString = new Gson().toJson(object);
            }
            afterString = afterString.replace(matcher.group(0), replaceString);
        }
        return afterString;
    }

    /**
     * 临时截图，供系统计算使用
     *
     * @return 图片路径
     */
    public String getScreenPic() {
        return DriverManager.get().baseDriver().getScreenshotAs(new ScreenShotType(
            Paths.get(Settings.TARGET_DIR, Settings.SCREENSHOT_TMP_DIR,
                mScenario.getName()).toString(),
            String.valueOf(System.currentTimeMillis()))).getAbsolutePath();
    }

    /**
     * 自动化测试截图，供测试脚本使用
     *
     * @param isAuto 是否为自动截图，false，用户主动触发的截图，true，系统自动帮忙截的图
     */
    public void screenshot(boolean isAuto) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        if (mScenario != null) {
            mScenario.embed(DriverManager.get().baseDriver().getScreenshotAs(OutputType.BYTES), "image/png");
            String path = Paths.get(Settings.TARGET_DIR, Settings.SCREENSHOT_DIR).toString();
            String fileName = mScenario.getName() + (isAuto ? "_auto" : "") + "_" + df.format(System.currentTimeMillis());
            DriverManager.get().baseDriver().getScreenshotAs(new ScreenShotType(path, fileName));
        }
    }

    public void tap(int x, int y) {
        if (Platform.isMobile()) {
            int offsetX = x;
            int offsetY = y;
            if (x <= 0) offsetX = 1;
            if (y <= 0) offsetY = 1;
            if (x >= Device.screenWidth) offsetX = Device.screenWidth - 1;
            if (y >= Device.screenHeight) offsetY = Device.screenHeight - 1;
            new TouchAction(DriverManager.get().mobileDriver())
                .tap(PointOption.point(offsetX, offsetY))
                .waitAction(WaitOptions.waitOptions(Duration.ofMillis(250)))
                .perform();
            return;
        }
        Actions act = new Actions(DriverManager.get().baseDriver());
        act.moveByOffset(x - currentMouse.getX(), y - currentMouse.getY())
            .click().build().perform();
    }

    public Scenario getScenario() {
        return mScenario;
    }

    @After
    public void afterTest() {
        if (mScenario == null) {
            return;
        }

        boolean notClose = false;
        for (String tag : mTagNames) {
            if (TagNames.NOT_CLOSE.equals(tag)) {
                notClose = true;
            } else if (tag.startsWith(TagNames.AFTER)) {
                String macroPath = tag.substring(TagNames.AFTER.length()).trim();
                Log.i("run @after " + macroPath);
                File macroFile = Paths.get(System.getProperty("user.dir"), macroPath + ".macro").toFile();
                if (macroFile.exists()) {
                    try {
                        String content = FileUtils.readFileToString(macroFile, "UTF-8");
                        Matcher matcher = Pattern.compile("\\*\\s+execute_script([^\\r]+?)\"{3}([^\\r]+?)\"{3}").matcher(content);
                        while (matcher.find()) {
                            executeScript(null, matcher.group(2).trim(), " " + matcher.group(1).trim());
                        }
                    } catch (Exception e) {
                        Log.e(macroFile.getAbsolutePath() + " execute failed", e);
                    }
                } else {
                    Log.e(macroFile.getAbsolutePath() + " not exist");
                }
            }
        }

        if (mScenario.isFailed() && DriverManager.get().baseDriver() != null) {
            screenshot(true);
        } else {
            try {
                String path = Paths.get(Settings.TARGET_DIR, Settings.SCREENSHOT_TMP_DIR,
                    mScenario.getName()).toString();
                FileUtils.deleteDirectory(new File(path));
            } catch (Exception e) {
                // ignore
            }
        }
        if (!Platform.isMobile() && !notClose
            && Platform.CHROME_DEBUGGING == null
            && BooleanUtils.isNotTrue(Global.DEBUGGER_SCENARIOS.get(mScenario.getName()))) {
            DriverManager.get().baseDriver().quit();
        }
    }

    @Web_Only
    public void refreshWindowSize() {
        Device.screenWidth = DriverManager.get().baseDriver().manage().window().getSize().getWidth();
        Device.screenHeight = DriverManager.get().baseDriver().manage().window().getSize().getHeight();
    }

    private int setViewportSize(int width, int height) {
        Object realHeight =
            DriverManager.get().baseDriver().executeScript(
                "return window.outerHeight == 0 ? arguments[0] : (window.outerHeight - window.innerHeight + arguments[0]);", height);
        int intH = Integer.parseInt(realHeight.toString());
        Dimension dimension = new Dimension(width, intH);
        DriverManager.get().baseDriver().manage().window().setSize(dimension);
        return intH;
    }

    public void executeScript(String key, String script, String arguments) {
        List<Object> params = new ArrayList<>();
        if (arguments != null) {
            String args = Avocado.get().replaceVariables(arguments);
            // 不包括空格，则贪婪匹配，匹配出 "{"a":123}" 这种场景，包括空格，则非贪婪
            Matcher paramsMather = Pattern.compile("(\\s+\"[^\\s]+\")|(\\s+\".+?\")|(\\s+-?[\\d.]+)|(\\s+\\[.+?])").matcher(args);
            while (paramsMather.find()) {
                String content = paramsMather.group().trim();
                if (content.startsWith("\"") && content.endsWith("\"")) {
                    content = content.substring(1, content.length() - 1);
                    params.add(content);
                } else if (content.startsWith("[") && content.endsWith("]")) {
                    String[] array = content.substring(1, content.length() - 1).split(",\\s+");
                    params.add(array);
                } else {
                    try {
                        params.add(Integer.parseInt(content, 10));
                    } catch (Exception e) {
                        params.add(content);
                    }
                }
            }
        }

        Avocado.get().sleep(Duration.ofMillis(1000));
        Object result = DriverManager.get().baseDriver().executeScript(script, params.toArray());
        if (result != null) {
            if (key != null) {
                Global.VARIABLES.put(key, result);
            }
        }
    }

    @Before
    public void connectDevice(Scenario scenario) throws Exception {
        mScenario = scenario;
        setCurrentMouse(0, 0);
        if (mScenario != null) {
            mTagNames.clear();
            mTagNames.addAll(mScenario.getSourceTagNames());
        }
        boolean noReset = false;
        boolean showProcess = false;

        for (String tagName : mTagNames) {
            if (TagNames.NO_RESET.equals(tagName)) {
                noReset = true;
            }
            if (tagName.startsWith(TagNames.VIEWPORT)) {
                Global.VARIABLES.put(GlobalKeys.SYSTEM_WINDOW_SIZE,
                    tagName.substring(TagNames.VIEWPORT.length()));
            }
            if (TagNames.SHOW_PROCESS.equals(tagName)) {
                showProcess = true;
            }
        }

        initTest(noReset, showProcess);
    }

    public void initTest(boolean noReset, boolean showProcess) throws IOException {
        Yaml yaml = new Yaml();
        DesiredCapabilities capabilities = new DesiredCapabilities();
        File configFile = Paths.get(Platform.SCRIPT_LOCATION, Settings.CONFIG_PATH).toFile();
        if (configFile.exists()) {
            System.out.println("find " + configFile.getAbsolutePath());
            Map<String, Object> values = yaml.load(new FileInputStream(configFile));

            if (values.get("platform") != null) {
                Platform.CURRENT_PLATFORM = String.valueOf(values.get("platform"));
            }
            System.out.println("Platform.CURRENT_PLATFORM: " + Platform.CURRENT_PLATFORM + ", start run " + mScenario.getName());

            if (Platform.SERVER_ADDRESS == null && values.get("server") != null) {
                Platform.SERVER_ADDRESS = String.valueOf(values.get("server"));
            }

            if (Platform.isMobile()) {
                Platform.DEVICE_NAME = String.valueOf(values.get("deviceName"));
                capabilities.setCapability("deviceName", Platform.DEVICE_NAME);
                capabilities.setCapability("platformName", Platform.CURRENT_PLATFORM);
                if (Platform.UDID == null) {
                    Platform.UDID = String.valueOf(values.get("udid"));
                }
                if (Platform.PACKAGE_LOCATION == null) {
                    Platform.PACKAGE_LOCATION = String.valueOf(values.get("app"));
                }
                capabilities.setCapability("udid", Platform.UDID);
                capabilities.setCapability("app", Platform.PACKAGE_LOCATION);
                if (noReset) {
                    capabilities.setCapability(MobileCapabilityType.NO_RESET, true);
                    capabilities.setCapability(MobileCapabilityType.FULL_RESET, false);
                }
            }
        }

        switch (Platform.CURRENT_PLATFORM) {
            case Platform.ANDROID:
                capabilities.setCapability("newCommandTimeout", 10000);
                capabilities.setCapability("automationName", "UiAutomator2");
                capabilities.setCapability("unicodeKeyboard", true);
                capabilities.setCapability("resetKeyboard", true);
                capabilities.setCapability("autoGrantPermissions", true);

                String keyStorePath = System.getenv("KEY_STORE_PATH");
                String password = System.getenv("ANDROID_KEY_PASSWORD");

                if (password != null && keyStorePath != null) {
                    capabilities.setCapability("useKeystore", true);
                    capabilities.setCapability("keystorePath", keyStorePath);
                    capabilities.setCapability("keyAlias", "qunhe.keystore");
                    capabilities.setCapability("keystorePassword", password);
                    capabilities.setCapability("keyPassword", password);
                }

                loadCustomCapabilities(yaml, capabilities);
                AndroidDriver<MobileElement> androidDriver = new AndroidDriver<>(new URL(Platform.SERVER_ADDRESS + "/wd/hub"),
                    capabilities);
                // 防止 Could not detect idle state
                androidDriver.setSetting(Setting.WAIT_FOR_IDLE_TIMEOUT, 0);
                DriverManager.get().setAndroidDriver(androidDriver);
                Device.screenWidth = androidDriver.manage().window().getSize().getWidth();
                Device.screenHeight = androidDriver.manage().window().getSize().getHeight();
                if (androidDriver.isDeviceLocked()) {
                    androidDriver.unlockDevice();
                    swipe(200, 1000, 200, 0);
                }
                break;
            case Platform.IOS:
                capabilities.setCapability("newCommandTimeout", 3000);
                capabilities.setCapability("unicodeKeyboard", true);
                capabilities.setCapability("resetKeyboard", true);
                capabilities.setCapability("autoGrantPermission", true);
                capabilities.setCapability("screenshotQuality", 2);
                capabilities.setCapability("xcodeOrgId", "BZM86V26D4");
                capabilities.setCapability("xcodeSigningId", "iPhone Developer");
                capabilities.setCapability("automationName", "XCuiTest");
                capabilities.setCapability("wdaStartupRetryInterval", 1000);
                capabilities.setCapability("useNewWDA", false);
                capabilities.setCapability("waitForQuiescence", false);
                capabilities.setCapability("shouldUseSingletonTestManager", false);
                loadCustomCapabilities(yaml, capabilities);
                IOSDriver<MobileElement> iosDriver = new IOSDriver<>(new URL(Platform.SERVER_ADDRESS + "/wd/hub"),
                    capabilities);
                iosDriver.context("NATIVE_APP");
                DriverManager.get().setIOSDriver(iosDriver);
                Device.screenWidth = iosDriver.manage().window().getSize().getWidth();
                Device.screenHeight = iosDriver.manage().window().getSize().getHeight();
                // 因为 iOS 使用的是逻辑像素，此处先截张图，然后算下比例
                String path = getScreenPic();
                File picture = new File(path);
                BufferedImage sourceImg = ImageIO.read(new FileInputStream(picture));
                Device.WIDTH_RATIO = (float) sourceImg.getWidth() / (float) Device.screenWidth;
                Device.HEIGHT_RATIO = (float) sourceImg.getHeight() / (float) Device.screenHeight;
                iosDriver.unlockDevice();
                break;
            default:
                RemoteWebDriver driver;

                ChromeOptions options = new ChromeOptions();
                if (!(showProcess || Platform.SHOW_PROCESS)) {
                    options.setHeadless(true);
                }
                options.addArguments("--disable-gpu");
                options.addArguments("--disable-dev-shm-usage");
                options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
                Map<String, Object> prefs = new HashMap<>();
                // 阻止弹窗
                prefs.put("credentials_enable_service", false);
                prefs.put("profile.password_manager_enabled", false);
                prefs.put("profile.default_content_setting_values.notifications", 2);
                options.setExperimentalOption("prefs", prefs);
                if (Platform.CHROME_DEBUGGING != null) {
                    options.addArguments(Platform.CHROME_DEBUGGING);
                }

                options.addArguments(String.format("window-size=%s", String.format("%d,%d", 1920, 1080)));

                options.setPageLoadStrategy(PageLoadStrategy.NONE);
                try {
                    options.addExtensions(Paths.get(getJarPath(), "assets", "selectors.crx").toFile());
                } catch (Exception e) {
                    // ignore
                }

                capabilities.setCapability(ChromeOptions.CAPABILITY, options);

                if (Platform.SERVER_ADDRESS != null) {
                    // 优先使用 server 的设置
                    driver = new RemoteWebDriver(new URL(Platform.SERVER_ADDRESS), options);
                } else if (Platform.CHROME_DRIVER_PATH != null) {
                    // 第二使用路径设置
                    Path driverPath;
                    if (Platform.CHROME_DRIVER_PATH.startsWith(".")) {
                        driverPath = Paths.get(System.getProperty("user.dir"), Platform.CHROME_DRIVER_PATH);
                    } else {
                        driverPath = Paths.get(Platform.CHROME_DRIVER_PATH);
                    }
                    File driverFile = driverPath.toFile();
                    if (driverFile.exists()) {
                        System.setProperty("webdriver.chrome.driver", driverFile.getAbsolutePath());
                    } else {
                        throw new RuntimeException("chromedriver not exist, please user --chromeDriver set");
                    }
                    driver = new ChromeDriver(capabilities);
                } else {
                    // 自动管理
                    WebDriverManager.globalConfig().setUseMirror(true).setAvoidAutoReset(true)
                        .setCachePath(Paths.get(Settings.TARGET_DIR, "driver").toString())
                        .setChromeDriverMirrorUrl(new URL("https://npm.taobao.org/mirrors/chromedriver/"));
                    System.out.println("Preparing Environment...Downloading Driver...Maybe it takes 5 min...");
                    switch (Platform.CURRENT_PLATFORM) {
                        case Platform.IE:
                            WebDriverManager.iedriver().setup();
                            driver = new InternetExplorerDriver(capabilities);
                            break;
                        case Platform.FIREFOX:
                            WebDriverManager.firefoxdriver().setup();
                            driver = new FirefoxDriver(capabilities);
                            break;
                        case Platform.EDGE:
                            WebDriverManager.edgedriver().setup();
                            driver = new EdgeDriver(capabilities);
                            break;
                        case Platform.SAFARI:
                            driver = new SafariDriver(capabilities);
                            break;
                        case Platform.OPERA:
                            WebDriverManager.operadriver().setup();
                            driver = new OperaDriver(capabilities);
                            break;
                        default:
                            WebDriverManager.chromedriver().setup();
                            driver = new ChromeDriver(capabilities);
                            break;
                    }
                }
                System.out.println("Environment has been set, start run " + mScenario.getName());

                DriverManager.get().setWebDriver(driver);

                if (Global.VARIABLES.get(GlobalKeys.SYSTEM_WINDOW_SIZE) != null) {
                    String[] size = Global.VARIABLES.get(GlobalKeys.SYSTEM_WINDOW_SIZE).toString().split(",");
                    int w = Integer.parseInt(size[0]);
                    int h = Integer.parseInt(size[1]);
                    int calcH = setViewportSize(w, h);
                    Dimension dimension = driver.manage().window().getSize();
                    if ((w != dimension.getWidth()) || calcH != dimension.getHeight()) {
                        throw new CucumberException(
                            String.format("@viewport:%d,%d, actual height is %d set, but current screen(%d,%d) cannot not fit",
                                w, h, calcH, dimension.getWidth(), dimension.getHeight()));
                    }
                }
                Dimension dimension = driver.manage().window().getSize();
                Device.screenHeight = dimension.getHeight();
                Device.screenWidth = dimension.getWidth();

                break;
        }
    }

    private String getJarPath() {
        String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        if (System.getProperty("os.name").contains("dows")) {
            path = path.substring(1);
        }
        if (path.contains("jar")) {
            path = path.substring(0, path.lastIndexOf("."));
            return path.substring(0, path.lastIndexOf("/"));
        }
        return path.replaceAll("target/(test-)?classes/", "");
    }

    private void loadCustomCapabilities(Yaml yaml, DesiredCapabilities capabilities) {
        try {
            String localPath = Paths.get(Platform.SCRIPT_LOCATION, Settings.Capabilities_File).toString();
            Map<String, String> map = yaml.load(new FileInputStream(localPath));
            for (String key : map.keySet()) {
                capabilities.setCapability(key, map.get(key));
            }
        } catch (FileNotFoundException e) {
            // ignore
        }
    }
}
