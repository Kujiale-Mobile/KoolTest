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

import com.qunhe.avocado.lib.annotations.Mobile_Only;
import com.qunhe.avocado.lib.annotations.Web_Only;
import com.qunhe.avocado.lib.constant.Schemes;
import com.qunhe.avocado.lib.constant.Settings;
import com.qunhe.avocado.lib.global.Global;
import com.qunhe.avocado.steps.NetStep;
import com.qunhe.avocado.steps.UiStep;
import cucumber.api.java.en.Given;
import org.junit.Assert;

/**
 * 语法定义类
 * @author CPPAlien
 */
public class KoolSteps {
    private final UiStep uiStep = new UiStep();
    private final NetStep netStep = new NetStep();

    // 判断元素是否存在，不存在则报错
    @Given("^(\\?)?assert_exists\\s+\"(?<!image://)(.+)\"(?:/([^\\s]+))?(?:\\s+(\\d+)s)?$")
    public void assert_exists(String optional, String arg, String index, Integer timeout) {
        if (timeout != null && timeout * 1000L > Settings.MAX_TIMEOUT) {
            Assert.fail("超时时间设置超过：" + Settings.MAX_TIMEOUT / 1000L + "秒");
        }
        uiStep.assert_exists(optional, arg, index, timeout);
    }

    @Given("^(\\?)?assert_exists\\s+\"image://([^\"]+)\"(?:\\s+(\\d+),(\\d+),(\\d+),(\\d+))?(?:\\s+(\\d+)s)?$")
    public void assert_image_exists(String optional, String arg, Integer startX, Integer startY, Integer width, Integer height, Integer timeout) throws Exception {
        if (timeout != null && timeout * 1000L > Settings.MAX_TIMEOUT) {
            Assert.fail("超时时间设置超过：" + Settings.MAX_TIMEOUT / 1000L + "秒");
        }
        netStep.screenshotAndCompare(optional, arg, startX, startY, width, height, timeout);
    }

    // 判断元素是否不存在，存在的话报错
    @Given("^assert_not_exists\\s+\"([^\"]*)\"(?:\\s+(\\d+)s)?$")
    public void assert_not_exists(String arg, Integer timeout) {
        if (timeout != null && timeout * 1000L > Settings.MAX_TIMEOUT) {
            Assert.fail("超时时间设置超过：" + Settings.MAX_TIMEOUT / 1000L + "秒");
        }
        uiStep.assert_not_exists(arg, timeout);
    }

    // 判断复选框是否勾选上
    // 注：一定需要为 input 结尾的 xpath
    @Given("^assert(_not)?_checked\\s+\"([^\"]*)\"$")
    public void assert_not_checked(String not, String arg) {
        uiStep.assert_not_checked(not, arg);
    }

    @Given("^check\\s+\"([^\"]*)\"$")
    public void check(String arg) {
        uiStep.check(arg);
    }

    @Given("^assert_text_equal\\s+\"([^\"]*)\"\\s+\"([^\"]*)\"")
    public void assertEqual(String first, String second) {
        uiStep.assertEqual(first, second);
    }

    @Web_Only
    @Given("^(?:([^\"\\s]+)\\s*=\\s*)?execute_script(\\s+.*)?$")
    public void executeScript(String key, String arguments, String docString) {
        uiStep.executeScript(key, docString, arguments);
    }

    @Given("^open\\s+\"([^\"]*)\"$")
    public void navigate_to(String url) {
        uiStep.navigate_to(url);
    }

    @Given("^(\\w+)\\s*=\\s*\"([^\"]*)\"")
    public void fetch_value(String key, String element) {
        if (!element.startsWith(Schemes.ELEMENT)) {
            Assert.fail("script format is error, you should use element://");
        }
        String value = uiStep.fetch_value(element);
        Global.VARIABLES.put(key, value);
    }

    // 点击某个点后，再移到另一个点
    @Given("^swipe\\s+((?:\\d)+(?:.?(?:\\d)+)?)\\s+((?:\\d)+(?:.?(?:\\d)+)?)\\s+((?:\\d)+(?:.?(?:\\d)+)?)\\s+((?:\\d)+(?:.?(?:\\d)+)?)$")
    public void swipe(double startX, double startY, double endX, double endY) {
        uiStep.swipe(startX, startY, endX, endY);
    }

    @Given("restart")
    public void restart() throws Exception {
        uiStep.restart();
    }

    // 点到某个 element 后，进行 向左，上，右，下 移动
    @Given("^swipe\\s+\"([^\"]*)\"\\s+(-?(?:\\d)+(?:.?(?:\\d)+)?)\\s+(-?(?:\\d)+(?:.?(?:\\d)+)?)$")
    public void swipe(String arg, double horizontal, double vertical) throws Exception {
        uiStep.swipe(arg, horizontal, vertical);
    }

    @Given("^(\\?)?input\\s+\"([^\"]*)\"\\s+to\\s+\"(.+)\"$")
    public void input(String optional, String content, String name) {
        uiStep.input(optional, content, name);
    }

    @Given("^(\\?)?input\\s+\"([^\"]*)\"\\s*$")
    public void inputOnly(String optional, String content) {
        uiStep.input(optional, content);
    }

    @Given("^upload\\s+\"([^\"]*)\"\\s*$")
    public void upload(String path) {
        uiStep.upload(path);
    }

    @Given("^(\\?)?(?:tap|click)\\s+\"([^\"]+)\"(?:/(-?\\w+))?$")
    public void tap(String optional, String arg, String indexString) throws Exception {
        uiStep.tap(optional, arg, indexString);
    }

    @Mobile_Only
    @Given("^assert_toast\\s+\"([^\"]*)\"$")
    public void assert_toast(String arg) {
        uiStep.assert_toast(arg);
    }

    // 判断元素是否存在，不存在则报错
    @Given("^(?:tap|click)\\s+([\\d.]+)\\s+([\\d.]+)$")
    public void tap_x_y(String x, String y) {
        uiStep.tap_x_y(x, y);
    }

    @Given("^(?:tap|click)\\s+\"([^\"]+)\"\\s+\"([^\"]+)\"$")
    public void tap_x_y_s(String x, String y) {
        uiStep.tap_x_y(x, y);
    }

    @Given("^alert\\s+(accept|dismiss)$")
    public void alert(String arg) {
        uiStep.alert(arg);
    }

    @Given("^wait\\s+(\\d+)s$")
    public void waitS(int arg0) {
        uiStep.waitS(arg0);
    }

    @Given("^screenshot\\s*(\\d)?$")
    public void screenshot(String countString) {
        uiStep.screenshot(countString);
    }

    // 判断元素是否存在，不存在则报错
    @Given("^assert_title_exists\\s+\"([^\"]*)\"$")
    public void assert_title_exists(String arg) {
        uiStep.assert_title_exists(arg);
    }

    @Web_Only
    @Given("^hover\\s+\"([^\"]*)\"(?:/(-?\\w+))?$")
    public void hover(String arg, String indexString) throws Exception {
        uiStep.hover(arg, indexString);
    }

    @Given("^mouse\\s+(down|move|up|context)(?:\\s+([^\\s]+)\\s+([^\\s]+))?$")
    public void mouse(String type, String x, String y) {
        uiStep.mouse(type, x, y);
    }

    @Given("^mouse\\s+down\\s+\"([^\"]*)\"$")
    public void mouseDown(String arg) {
        uiStep.mouseDown(arg);
    }

    @Given("^mouse\\s+wheel\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)$")
    public void mouseWheel(String x, String y, String deltaY) {
        uiStep.mouseWheel(x, y, deltaY);
    }

    @Given("^window\\s+(maximize|fullscreen|close)")
    public void window(String type) throws Exception {
        uiStep.window(type);
    }

    @Given("^scroll\\s+(-?(?:\\d+))\\s+(-?(?:\\d+))$")
    public void scroll(int x, int y) {
        uiStep.scroll(x, y);
    }

    @Given("^scroll_to\\s+\"([^\"]+)\"$")
    public void scroll_to(String arg) {
        uiStep.scroll_to(arg);
    }

    //// API请求相关
    @Given("^((\\w+)\\s*=\\s*)?(\\?)?do_get\\s+\"([^\"]*)\"(?:\\s+(\\w+))?$")
    public void do_get(String key, String optional, String url, String status, String docString) throws Exception {
        netStep.doRequest("GET", key, url, status, docString, optional != null);
    }

    @Given("^(?:(\\w+)\\s*=\\s*)?(\\?)?do_post\\s+\"([^\"]*)\"(?:\\s+(\\w+))?$")
    public void do_post(String key, String optional, String url, String status, String docString) throws Exception {
        netStep.doRequest("POST", key, url, status, docString, optional != null);
    }

    @Given("^(?:(\\w+)\\s*=\\s*)?(\\?)?do_delete\\s+\"([^\"]*)\"(?:\\s+(\\w+))?$")
    public void do_delete(String key, String optional, String url, String status, String docString) throws Exception {
        netStep.doRequest("DELETE", key, url, status, docString, optional != null);
    }

    @Given("^(?:(\\w+)\\s*=\\s*)?(\\?)?do_put\\s+\"([^\"]*)\"(?:\\s+(\\w+))?$")
    public void do_put(String key, String optional, String url, String status, String docString) throws Exception {
        netStep.doRequest("PUT", key, url, status, docString, optional != null);
    }

    @Given("^(\\?)?keyboard\\s+(.*)$")
    public void keyboard(String optional, String arg) throws Exception {
        uiStep.keyboard(optional, arg);
    }

    @Given("^key\\s+(down|up)\\s+\"([^\"]*)\"$")
    public void keyDownUp(String type, String arg) {
        uiStep.keyDownUp(type, arg);
    }

    @Given("^macro")
    public void macro() {

    }
}
