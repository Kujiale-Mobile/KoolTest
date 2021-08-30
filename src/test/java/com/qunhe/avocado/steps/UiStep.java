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
package com.qunhe.avocado.steps;

import com.qunhe.avocado.lib.Avocado;
import com.qunhe.avocado.lib.DriverManager;
import com.qunhe.avocado.lib.annotations.Mobile_Only;
import com.qunhe.avocado.lib.annotations.Web_Only;
import com.qunhe.avocado.lib.constant.Settings;
import com.qunhe.avocado.lib.constant.XPath;
import com.qunhe.avocado.lib.global.Platform;
import com.qunhe.avocado.lib.constant.Schemes;
import com.qunhe.avocado.lib.model.Device;
import com.qunhe.avocado.lib.model.MatchRect;
import com.qunhe.avocado.lib.util.Log;
import cucumber.runtime.CucumberException;
import io.appium.java_client.MobileElement;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.Point;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static junit.framework.TestCase.*;

/**
 * @author CPPAlien
 */
public class UiStep {
    public void assert_exists(String optional, String arg, String index, Integer timeout) {
        try {
            private_assert_exists(optional, arg, index, timeout);
        } catch (StaleElementReferenceException e) {
            private_assert_exists(optional, arg, index, timeout);
        }
    }

    // 判断元素是否存在，不存在则报错
    private void private_assert_exists(String optional, String arg, String index, Integer timeout) {
        long realTimeout = timeout == null ? (optional == null ?
            Settings.TIMEOUT : Settings.OPTIONAL_TIMEOUT) : timeout * 1000L;
        long startTime = System.currentTimeMillis();
        while (true) {
            boolean isSuccess = false;
            List<WebElement> webElements;
            if (arg.startsWith(Schemes.ELEMENT)) {
                webElements = Avocado.get().findElementsBySelector(arg);
            } else {
                webElements = Avocado.get().findElementsByText(arg, true);
            }
            String errorMessage = null;
            if (index != null) {
                Matcher matcher = Pattern.compile("([>=<]=?)?(\\d+)").matcher(index);
                if (matcher.matches()) {
                    errorMessage = matcher.group(0);
                    int expectedCount = Integer.parseInt(matcher.group(2));
                    String tag = matcher.group(1) == null ? "" : matcher.group(1);
                    switch (tag) {
                        case "<":
                            isSuccess = webElements.size() > 0 && (expectedCount > webElements.size());
                            break;
                        case "<=":
                            isSuccess = webElements.size() > 0 && (expectedCount >= webElements.size());
                            break;
                        case ">":
                            isSuccess = (expectedCount < webElements.size());
                            break;
                        case ">=":
                            isSuccess = (expectedCount <= webElements.size());
                            break;
                        default:
                            isSuccess = (expectedCount == webElements.size());
                            break;
                    }
                } else {
                    for (WebElement element : webElements) {
                        if (StringUtils.equals(element.getTagName(), index)) {
                            isSuccess = true;
                        }
                    }
                }
            } else {
                isSuccess = webElements.size() > 0;
            }
            if (isSuccess) {
                break;
            }
            if (System.currentTimeMillis() - startTime > realTimeout) {
                if (optional == null) {
                    if (webElements.isEmpty()) {
                        Assert.fail("Could not find " + arg);
                    } else if (errorMessage != null) {
                        Assert.fail(String.format("Found %d of %s, but expected %s", webElements.size(), arg, errorMessage));
                    } else if (index != null) {
                        Assert.fail(String.format("Could not find %s/%s", arg, index));
                    } else {
                        Assert.fail();
                    }
                }
                break;
            }
        }
    }

    public void assert_not_exists(String arg, Integer timeout) {
        try {
            private_assert_not_exists(arg, timeout);
        } catch (StaleElementReferenceException e) {
            private_assert_not_exists(arg, timeout);
        }
    }

    private void private_assert_not_exists(String arg, Integer timeout) {
        long timeoutInt = timeout == null ? Settings.TIMEOUT : timeout * 1000L;
        long startTime = System.currentTimeMillis();
        Avocado.get().sleep(Duration.ofSeconds(1));
        do {
            List<WebElement> webElements;
            if (arg.startsWith(Schemes.ELEMENT)) {
                webElements = Avocado.get().findElementsBySelector(arg);
            } else {
                webElements = Avocado.get().findElementsByText(arg, true);
            }
            if (webElements.size() == 0) {
                Assert.assertTrue(true);
                return;
            }
        } while (System.currentTimeMillis() - startTime <= timeoutInt);
        Assert.fail("after " + timeoutInt / 1000 + "s, " + arg + " still exists");
    }

    private List<WebElement> findCheckElements(String selector, long timeout) {
        long startTime = System.currentTimeMillis();
        List<WebElement> elements = new ArrayList<>();
        while (true) {
            Avocado.get().sleep(Duration.ofMillis(500));
            elements.addAll(Avocado.get().findElementsBySelector(selector));
            elements = elements.stream()
                .filter(e -> "input".equals(e.getTagName()) &&
                    ("checkbox".equals(e.getAttribute("type")) || "radio".equals(e.getAttribute("type"))))
                .collect(Collectors.toList());
            if (elements.size() > 0) {
                return elements;
            }
            if (System.currentTimeMillis() - startTime > timeout) {
                return elements;
            }
        }
    }

    @Web_Only
    public void check(String arg) {
        List<WebElement> elements;
        if (arg.startsWith(Schemes.ELEMENT)) {
            elements = findCheckElements(arg, Settings.TIMEOUT);
        } else {
            String selector = String.format("element://xpath/%s", XPath.WEB_ELEMENT_CHECK_ONE).replaceAll("%s", arg);
            elements = findCheckElements(selector, 5 * 1000);
            if (elements.isEmpty()) {
                selector = String.format("element://xpath/%s", XPath.WEB_ELEMENT_CHECK_TWO).replaceAll("%s", arg);
                elements = findCheckElements(selector, 5 * 1000);
                if (elements.isEmpty()) {
                    selector = String.format("element://xpath/%s", XPath.WEB_ELEMENT_CHECK_THREE).replaceAll("%s", arg);
                    elements = findCheckElements(selector, 5 * 1000);
                }
            }
        }
        if (elements.isEmpty()) {
            Assert.fail("Could not find " + arg + " as input[type = 'checkbox' or type = 'radio']");
        }
        for (WebElement element : elements) {
            try {
                element.click();
            } catch (ElementClickInterceptedException e) {
                Actions actions = new Actions(DriverManager.get().baseDriver());
                actions.moveToElement(element).click().perform();
            }
        }
    }

    public void assert_not_checked(String not, String arg) {
        List<WebElement> elements;
        if (arg.startsWith(Schemes.ELEMENT)) {
            elements = findCheckElements(arg, Settings.TIMEOUT);
        } else {
            String selector = String.format("element://xpath/%s", XPath.WEB_ELEMENT_CHECK_ONE).replaceAll("%s", arg);
            elements = findCheckElements(selector, 5 * 1000);
            if (elements.isEmpty()) {
                selector = String.format("element://xpath/%s", XPath.WEB_ELEMENT_CHECK_TWO).replaceAll("%s", arg);
                elements = findCheckElements(selector, 5 * 1000);
                if (elements.isEmpty()) {
                    selector = String.format("element://xpath/%s", XPath.WEB_ELEMENT_CHECK_THREE).replaceAll("%s", arg);
                    elements = findCheckElements(selector, 5 * 1000);
                }
            }
        }
        if (elements.isEmpty()) {
            Assert.fail("Could not find " + arg + " as input[type = 'checkbox' or type = 'radio']");
        }
        for (WebElement element : elements) {
            if (element.isSelected() && not != null) {
                Assert.fail(arg + " was checked");
            }
            if (!element.isSelected() && not == null) {
                Assert.fail(arg + " was not checked");
            }
        }
    }

    public void assertEqual(String first, String second) {
        String one = Avocado.get().replaceVariables(first);
        String two = Avocado.get().replaceVariables(second);
        if (one.startsWith(Schemes.ELEMENT)) {
            one = fetch_value(one);
        }
        if (two.startsWith(Schemes.ELEMENT)) {
            two = fetch_value(two);
        }
        Assert.assertEquals(one, two);
    }

    @Web_Only
    public void executeScript(String key, String script, String arguments) {
        Avocado.get().executeScript(key, script, arguments);
    }

    public void navigate_to(String url) {
        Avocado.get().open(url);
        if (!Platform.isMobile()) {
            switchTab();
        }
    }

    public String fetch_value(String element) {
        try {
            List<WebElement> elements = Avocado.get().findElement(element, Settings.TIMEOUT);
            WebElement webElement = elements.get(0);
            String tagName = webElement.getTagName();
            String value;
            if ("input".equals(tagName)) {
                value = webElement.getAttribute("value");
            } else {
                value = webElement.getText();
            }
            return value;
        } catch (StaleElementReferenceException e) {
            return fetch_value(element);
        }
    }

    // 点击某个点后，再移到另一个点
    public void swipe(double startX, double startY, double endX, double endY) {
        Avocado.get().sleep(Duration.ofSeconds(1));
        Avocado.get().swipe((int) (startX * Device.screenWidth), (int) (startY * Device.screenHeight),
            (int) (endX * Device.screenWidth), (int) (endY * Device.screenHeight));
    }

    @Mobile_Only
    public void restart() throws Exception {
        Avocado.get().closeApp();
        Avocado.get().initTest(true, true);
    }

    // 点到某个 element 后，进行 向左，上，右，下 移动
    public void swipe(String arg, double horizontal, double vertical) throws Exception {
        MatchRect.Point center = Avocado.get().findElementCenter(arg).get(0);
        int endX = (int) (center.X + Device.screenWidth * horizontal);
        if (endX < 0) {
            endX = 0;
        } else if (endX > Device.screenWidth) {
            endX = Device.screenWidth;
        }
        int endY = (int) (center.Y + Device.screenHeight * vertical);
        if (endY < 0) {
            endY = 0;
        } else if (endY > Device.screenHeight) {
            endY = Device.screenHeight;
        }
        Avocado.get().swipe(center.X, center.Y, endX, endY);
    }

    public void keyboard(String optional, String arg) throws ClassNotFoundException, IllegalAccessException {
        Long timeout = optional != null ? Settings.OPTIONAL_TIMEOUT : Settings.TIMEOUT;
        List<WebElement> elements = Avocado.get().findInputElementsByName("", timeout);
        for (WebElement element : elements) {
            if (element.equals(DriverManager.get().baseDriver().switchTo().activeElement())) {
                Object[] keys =
                    Arrays.stream(arg.split("\\s+")).map((s -> s.substring(1, s.length() - 1))).toArray();
                List<CharSequence> charSequenceList = new ArrayList<>();
                for (Object key : keys) {
                    try {
                        Class keyClass = Class.forName("org.openqa.selenium.Keys");
                        Field keyField = keyClass.getField(key.toString());
                        charSequenceList.add((CharSequence) keyField.get(Keys.class));
                    } catch (NoSuchFieldException e) {
                        charSequenceList.add((CharSequence) key);
                    }
                }
                element.sendKeys(Keys.chord(charSequenceList));
                return;
            }
        }
        if (optional == null) {
            throw new RuntimeException("Not found focused input element");
        }
    }

    public void upload(String path, Integer index) {
        path = Avocado.get().replaceVariables(path);
        long startTime = System.currentTimeMillis();
        List<WebElement> elements;
        while (true) {
            elements = Avocado.get().findWebElements(By.cssSelector("input[type='file']"), false);
            if (elements.size() > 0) {
                break;
            }
            if (System.currentTimeMillis() - startTime > Settings.TIMEOUT) {
                Assert.fail("Cannot found an upload element");
            }
        }

        StringBuilder allPath = new StringBuilder();
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null || files.length < 1) {
                Assert.fail("Cannot find file");
            }
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    allPath.append(files[i].getAbsolutePath());
                    if (i < files.length - 1) {
                        allPath.append("\n");
                    }
                }
            }
        } else {
            allPath.append(file.getAbsolutePath());
        }
        if (allPath.toString().length() == 0) {
            Assert.fail("Cannot find file");
        }
        int elementIndex = 0;
        if (index != null) {
            elementIndex = index;
        }
        WebElement inputElement = elements.get(elementIndex);
        DriverManager.get().baseDriver().executeScript("arguments[0].value = null;", inputElement);
        inputElement.sendKeys(allPath.toString());
    }

    public void input(String optional, String content) {
        Long timeout = optional != null ? Settings.OPTIONAL_TIMEOUT : Settings.TIMEOUT;
        List<WebElement> elements = Avocado.get().findInputElementsByName("", timeout);
        for (WebElement element : elements) {
            if (element.equals(DriverManager.get().baseDriver().switchTo().activeElement())) {
                if (!content.isEmpty()) {
                    content = Avocado.get().replaceVariables(content);
                    element.sendKeys(content);
                } else {
                    int textSize = element.getAttribute("value").length() + 1;
                    while (textSize > 0) {
                        element.sendKeys(Keys.BACK_SPACE);
                        textSize--;
                    }
                }
                return;
            }
        }
        if (optional == null) {
            throw new RuntimeException("Not found focused input element");
        }
    }

    public void input(String optional, String content, String name) {
        try {
            Long timeout = optional != null ? Settings.OPTIONAL_TIMEOUT : Settings.TIMEOUT;
            content = Avocado.get().replaceVariables(content);
            List<WebElement> elements;
            if (name.startsWith(Schemes.ELEMENT)) {
                elements = Avocado.get().findElement(name, timeout);
            } else {
                elements = Avocado.get().findInputElementsByName(name, timeout);
            }
            if (elements == null) {
                throw new NotFoundException(name);
            }
            int failCount = 0;
            for (WebElement element : elements) {
                try {
                    if (!content.isEmpty()) {
                        element.sendKeys(content);
                    } else {
                        int textSize = element.getAttribute("value").length() + 1;
                        while (textSize > 0) {
                            element.sendKeys(Keys.BACK_SPACE);
                            textSize--;
                        }
                    }
                } catch (ElementNotInteractableException e) {
                    Log.w("cannot send key to " + element.getTagName());
                    failCount++;
                }
            }
            if (failCount >= elements.size()) {
                Assert.fail("Found " + elements.size() + ", but neither enabled");
            }
        } catch (StaleElementReferenceException e) {
            // 出现该错误后，可以重试
            // https://stackoverflow.com/questions/16166261/selenium-webdriver-how-to-resolve-stale-element-reference-exception
            input(optional, content, name);
        } catch (Exception e) {
            if (optional == null) {
                throw e;
            }
        }
    }

    public List<WebElement> findElementsWithIndex(String arg, String indexString, @NotNull Long timeout) {
        long startTime = System.currentTimeMillis();
        while (true) {
            List<WebElement> elements;
            if (arg.startsWith(Schemes.ELEMENT)) {
                elements = Avocado.get().findElementsBySelector(arg);
            } else {
                elements = Avocado.get().findElementsByText(arg, false);
            }
            List<WebElement> filteredElements = new ArrayList<>();
            if (indexString != null) {
                try {
                    int index = Integer.parseInt(indexString);
                    int total = elements.size();
                    if (index < 0 && index + total >= 0) {
                        filteredElements.add(elements.get(index + total));
                    } else if (index < total) {
                        filteredElements.add(elements.get(index));
                    }
                } catch (Exception e) {
                    for (WebElement element : elements) {
                        if (StringUtils.equals(element.getTagName(), indexString)) {
                            filteredElements.add(element);
                        }
                    }
                }
            } else {
                filteredElements.addAll(elements);
            }
            if (filteredElements.size() > 0) {
                return filteredElements;
            }
            if (System.currentTimeMillis() - startTime > timeout) {
                StringBuilder message = new StringBuilder("Not found " + arg + (indexString != null ? "/" + indexString : ""));
                for (WebElement element : elements) {
                    message.append(", But we found ")
                        .append(element.getText())
                        .append("/")
                        .append(element.getTagName());
                }
                message.append("\n");
                throw new NotFoundException(message.toString());
            }
        }
    }

    public void tap(String optional, String arg, String indexString) throws Exception {
        Long timeout = optional != null ? Settings.OPTIONAL_TIMEOUT : Settings.TIMEOUT;
        try {
            if (!Platform.isMobile()) {
                Avocado.get().sleep(Duration.ofMillis(1000));
            }
            if (arg.startsWith(Schemes.IMAGE)) {
                List<MatchRect.Point> centers = Avocado.get().findElementCenter(arg);
                for (MatchRect.Point center : centers) {
                    Avocado.get().tap(center.X, center.Y);
                }
            } else {
                List<WebElement> filteredElements = findElementsWithIndex(arg, indexString, timeout);

                int index = 0;
                List<WebElement> notEnabledElements = new ArrayList<>();
                for (WebElement element : filteredElements) {
                    index++;
                    try {
                        if (element.isEnabled() && element.isDisplayed()) {
                            element.click();
                        } else {
                            notEnabledElements.add(element);
                        }
                    } catch (WebDriverException e) {
                        if (e instanceof ElementClickInterceptedException) {
                            // 有个坑，span 过了 element.isEnabled() && element.isDisplayed() 但却无法点击，改用点位置
                            // https://stackoverflow.com/questions/11908249/debugging-element-is-not-clickable-at-point-error
                            Actions actions = new Actions(DriverManager.get().baseDriver());
                            try {
                                actions.moveToElement(element).click().perform();
                            } catch (Exception exception) {
                                // 还是点击不成功，则直接忽略掉
                                Log.w(String.format("element %s, index %d, tap failed, total: %d", arg, index, filteredElements.size()));
                            }
                        } else {
                            Log.i(String.format("element %s, index %d, tap failed, total: %d", arg, index, filteredElements.size()));
                        }
                    }
                    Avocado.get().sleep(Duration.ofMillis(200));
                }
                if (notEnabledElements.size() > 0 && filteredElements.size() == notEnabledElements.size()) {
                    // 证明所有找到的元素都不可直接点击，按位置点击一下
                    for (WebElement element : notEnabledElements) {
                        Point point = element.getRect().getPoint();
                        Avocado.get().tap(point.x, point.y);
                    }
                }
            }
            if (!Platform.isMobile()) {
                switchTab();
            }
        } catch (StaleElementReferenceException e) {
            tap(optional, arg, indexString);
        } catch (Exception e) {
            // 是否必须，不必须的，则不用中断测试
            if (optional == null) {
                throw e;
            }
        }
    }

    @Mobile_Only
    public void assert_toast(String arg) {
        try {
            final WebDriverWait wait = new WebDriverWait(DriverManager.get().baseDriver(), Settings.TIMEOUT / 1000);
            String[] toasts = arg.split("\\|");
            if (Platform.CURRENT_PLATFORM.equals(Platform.IOS)) {
                long startTime = System.currentTimeMillis();
                while (true) {
                    Avocado.get().sleep(Duration.ofMillis(200));
                    List<MobileElement> elements = new ArrayList<>();
                    for (String toast : toasts) {
                        elements.addAll(DriverManager.get().iOSDriver().findElementsByAccessibilityId(toast));
                    }
                    if (elements.size() > 0) {
                        return;
                    }
                    if (System.currentTimeMillis() - startTime > Settings.TIMEOUT) {
                        throw new Exception();
                    }
                }
            } else {
                StringBuilder xpathBuffer = new StringBuilder(String.format(".//*[contains(@text,'%s')]", toasts[0]));
                if (toasts.length > 1) {
                    for (int i = 1; i < toasts.length; ++i) {
                        xpathBuffer.append(" | ");
                        xpathBuffer.append(String.format(".//*[contains(@text,'%s')]", toasts[i]));
                    }
                }
                Assert.assertNotNull(wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpathBuffer.toString()))));
            }
        } catch (Exception e) {
            throw new AssertionError("cannot found " + arg);
        }
    }

    // 判断元素是否存在，不存在则报错
    public void tap_x_y(String sX, String sY) {
        double x, y;
        try {
            String aSX = Avocado.get().replaceVariables(sX).replaceAll("[^\\d.]", "");
            String aSY = Avocado.get().replaceVariables(sY).replaceAll("[^\\d.]", "");
            x = Double.parseDouble(aSX);
            y = Double.parseDouble(aSY);
        } catch (Exception e) {
            throw new CucumberException("请设置正确的 x y 坐标， 注意当坐标 <= 1 时，会使用相对屏幕大小计算，如 0.5 0.5 则点击屏幕中央");
        }
        if (x <= 1) {
            x = Device.screenWidth * x;
        }
        if (y <= 1) {
            y = Device.screenHeight * y;
        }
        Avocado.get().tap((int) x, (int) y);
    }

    public void alert(String arg) {
        try {
            Avocado.get().autoAcceptAlertIfExists(arg);
        } catch (Exception e) {
            Log.w("alert " + arg + " failed");
        }
    }

    public void waitS(int arg0) {
        // Write code here that turns the phrase above into concrete actions
        Avocado.get().sleep(Duration.ofSeconds(arg0));
    }

    public void screenshot(String countString) {
        // Write code here that turns the phrase above into concrete actions
        int count = countString == null ? 1 : Integer.parseInt(countString);
        for (int i = 0; i < count; i++) {
            Avocado.get().screenshot(false);
            Avocado.get().sleep(Duration.ofMillis(500));
        }
    }

    // 判断元素是否存在，不存在则报错
    public void assert_title_exists(String arg) {
        assertTrue(Avocado.get().findTitleExists(arg, -1));
    }

    @Web_Only
    public void hover(String arg, String indexString) throws Exception {
        Avocado.get().sleep(Duration.ofMillis(1000));
        try {
            if (arg.startsWith(Schemes.IMAGE)) {
                List<MatchRect.Point> centers = Avocado.get().findElementCenter(arg);
                for (MatchRect.Point center : centers) {
                    Actions act = new Actions(DriverManager.get().baseDriver());
                    act.moveByOffset(center.X, center.Y).build().perform();
                    Avocado.get().setCurrentMouse(center.X, center.Y);
                }
            } else {
                List<WebElement> elements = findElementsWithIndex(arg, indexString, Settings.TIMEOUT);

                for (WebElement element : elements) {
                    Actions act = new Actions(DriverManager.get().baseDriver());
                    act.moveToElement(element).perform();
                    Rectangle rect = element.getRect();
                    Avocado.get().setCurrentMouse(rect.getX() + rect.width / 2, rect.getY() + rect.height / 2);
                }
            }
        } catch (StaleElementReferenceException e) {
            hover(arg, indexString);
        }
    }

    public void keyDownUp(String type, String arg) {
        Actions act = new Actions(DriverManager.get().baseDriver());
        Keys[] MODIFIER_KEYS = new Keys[]
            {Keys.SHIFT, Keys.CONTROL, Keys.ALT, Keys.META, Keys.COMMAND, Keys.LEFT_ALT, Keys.LEFT_CONTROL, Keys.LEFT_SHIFT};
        CharSequence key;
        try {
            Class keyClass = Class.forName("org.openqa.selenium.Keys");
            Field keyField = keyClass.getField(arg.toUpperCase());
            key = (CharSequence) keyField.get(Keys.class);
            if (Arrays.binarySearch(MODIFIER_KEYS, key) < 0) {
                act.sendKeys(key).build().perform();
                return;
            }
            switch (type) {
                case "down":
                    act.keyDown(key).build().perform();
                    break;
                case "up":
                    act.keyUp(key).build().perform();
                    break;
            }
        } catch (NoSuchFieldException | ClassNotFoundException | IllegalAccessException e) {
            if ("down".equals(type)) {
                act.sendKeys(arg).build().perform();
            }
        }
    }

    public void mouseDown(String arg) {
        List<WebElement> elements = findElementsWithIndex(arg, null, Settings.TIMEOUT);
        Actions act = new Actions(DriverManager.get().baseDriver());
        act.moveToElement(elements.get(0)).clickAndHold().build().perform();
    }

    public void mouseWheel(String sX, String sY, String deltaY) {
        sX = Avocado.get().replaceVariables(sX);
        sY = Avocado.get().replaceVariables(sY);
        deltaY = Avocado.get().replaceVariables(deltaY);
        float pointX, pointY, scrollY;
        pointX = Float.parseFloat(sX.replaceAll("\"", ""));
        pointY = Float.parseFloat(sY.replaceAll("\"", ""));
        scrollY = Float.parseFloat(deltaY.replaceAll("\"", ""));
        String script = "" +
            "let deltaY = arguments[2]\n" +
            "function wheel(x, y) {\n" +
            "  let arr = document.elementsFromPoint(x, y)\n" +
            "  for (let i = 0; i < arr.length; i++) {\n" +
            "    if (arr[i].nodeName === 'CANVAS') {\n" +
            "      return arr[i]\n" +
            "    }\n" +
            "    let overflow = getComputedStyle(arr[i]).overflow\n" +
            "    if (overflow.indexOf('overlay') >= 0 || overflow.indexOf('auto') >= 0 || overflow.indexOf('scroll') >= 0) {\n" +
            "      return arr[i];\n" +
            "    }\n" +
            "  }\n" +
            "}\n" +
            "\n" +
            "const element = wheel(arguments[0], arguments[1]);\n" +
            "if (!element) {\n" +
            "  window.scrollBy(0, deltaY);\n" +
            "} else if (element.nodeName !== 'CANVAS') {\n" +
            "  element.scrollTop += deltaY\n" +
            "} else {\n" +
            "  element.dispatchEvent(new WheelEvent('wheel', {view: window, bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1], deltaY}))\n" +
            "}" +
            "";
        DriverManager.get().baseDriver().executeScript(script, pointX, pointY, scrollY);
    }

    public void mouse(String type, String sX, String sY) {
        int x = 0, y = 0;
        int preX = Avocado.get().getCurrentMouse().getX();
        int preY = Avocado.get().getCurrentMouse().getY();
        if (sX != null && sY != null) {
            String aSX = Avocado.get().replaceVariables(sX).replaceAll("[^\\d.]", "");
            String aSY = Avocado.get().replaceVariables(sY).replaceAll("[^\\d.]", "");
            x = Double.valueOf(aSX).intValue();
            y = Double.valueOf(aSY).intValue();
        } else {
            if ("move".equals(type) || "context".equals(type)) {
                throw new RuntimeException("请设置 mouse move 的 x y 坐标");
            }
        }
        Avocado.get().sleep(Duration.ofMillis(500));
        Actions act = new Actions(DriverManager.get().baseDriver());
        switch (type) {
            case "down":
                act.clickAndHold().build().perform();
                break;
            case "up":
                act.release().build().perform();
                break;
            case "move":
                try {
                    act.moveByOffset(x - preX, y - preY).build().perform();
                    Avocado.get().setCurrentMouse(x, y);
                } catch (MoveTargetOutOfBoundsException e) {
                    // ignore
                }
                break;
            case "context":
                act.moveByOffset(x - preX, y - preY)
                    .contextClick().build().perform();
                Avocado.get().setCurrentMouse(x, y);
                break;
        }
    }

    @Web_Only
    public void window(String type) throws Exception {
        switch (type) {
            case "maximize":
                DriverManager.get().baseDriver().manage().window().maximize();
                Avocado.get().refreshWindowSize();
                break;
            case "fullscreen":
                DriverManager.get().baseDriver().manage().window().fullscreen();
                Avocado.get().refreshWindowSize();
                break;
            case "close":
                DriverManager.get().baseDriver().close();
                break;
            default:
                throw new Exception();

        }
    }

    public void scroll(int x, int y) {
        DriverManager.get().baseDriver().executeScript(String.format("scrollBy(%d, %d)", x, y));
    }

    public void scroll_to(String arg) {
        try {
            List<WebElement> elements = findElementsWithIndex(arg, null, Settings.TIMEOUT);
            DriverManager.get().baseDriver().executeScript("arguments[0].scrollIntoView(true);", elements.get(0));
        } catch (StaleElementReferenceException e) {
            scroll_to(arg);
        }
    }

    private void switchTab() {
        Set<String> browserTabs = DriverManager.get().baseDriver().getWindowHandles();
        browserTabs.removeAll(mBrowserTabs);
        if (browserTabs.isEmpty()) {
            return;
        }
        try {
            String handleId = null;
            for (String id : browserTabs) {
                handleId = id;
            }
            DriverManager.get().baseDriver().switchTo().window(handleId);
            Avocado.get().sleep(Duration.ofSeconds(1));
        } catch (Exception e) {
            Log.w("switch tab failed");
            // ignore
        }
        mBrowserTabs = DriverManager.get().baseDriver().getWindowHandles();
    }

    private Set<String> mBrowserTabs = new LinkedHashSet<>();
}
