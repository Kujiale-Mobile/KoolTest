# KoolTest UI 自动化测试框架

为了给用户提供更优质的产品体验，在产品上线前需要进行各项测试。其中回归测试多由测试人员手动执行，耗费了大量人力，并且还可能存在漏测问题。鉴于此，我们在跨端的 UI 自动化上面做了大量的优化和思考，实现了 KoolTest 的 跨端 UI 自动化测试方案，用以降低人力成本。目前本框架支持使用一套脚本规范来测试 Android、iOS、Web。



## 基本原理

![](https://qhstaticssl.kujiale.com/newt/165/image/png/1629281227269/C942720CFE68A7305D4EB8EBCD995E9D.png)

虽然 iOS、Android、Web 不同，但他们在元素查找、元素操作方式上有一定的共性，当然也有很多区别。比如 iOS 和 Android 支持 AccessibilityId 定位，而 Web 不支持；再比如 Web 支持的 class 方式查找，客户端平台不支持等，并且 iOS 和 Android 本身也有不一样的地方。所以我们在实现同时面向三者的测试平台时，需要特别注意这些差异的地方，尽量使用共性的部分，在使用平台独有功能时，在代码中进行平台判断。我们把其三者的关系梳理如下，其中 AppiumDriver 是客户端测试共性的类，基于 Selenium 提供的 RemoteWebDriver 实现，iOS 和 Android 的 Driver 基于 AppiumDriver 实现。

![](https://qhstaticssl.kujiale.com/newt/165/image/png/1629282336357/54F4C33A4F569F794E34C3D3B4BF3A14.png)

在 Client 之上，我们基于 Cucumber 实现了一套 BDD+关键字风格的测试描述语言（Kool Lang）。如下这个脚本，可以执行在所有终端，只要文案一致。

```
Feature: 登陆测试
  Scenario: 登录失败
    * open "https://www.kujiale.com"
    * tap "我的设计"
    * input "test@test.com" to "手机号/邮箱"
    * input "111111" to "密码"
    * tap "登录"
    * assert_exists "账号密码错误"
```



## 快速开始

**安装 java 环境**

https://docs.oracle.com/goldengate/1212/gg-winux/GDRAD/java.htm#BGBFJHAB



**安装 VSCode，并安装 kool-test-script 插件**

https://code.visualstudio.com/

![](https://qhstaticssl.kujiale.com/newt/165/image/png/1620440762840/48F1331DE7F02091AC40A183B083DF95.png)



**开始书写**

直接使用 VsCode 创建一个空项目，增加一个 .feature 文件，比如

![](https://qhstaticssl.kujiale.com/newt/165/image/png/1629283979942/45885072951456B290D39995F2B059A8.png)

点击 ***脚本回放***（首次运行因为要下载对应环境配置，所以会慢一些），则直接会运行该脚本，如果需要打断点调试，可以在某个步骤中插入 debugger 标签（注意 debugger 标签不要放在 Scenario 第一行，也不要放在 Background 中），再点击运行回放时，网页会停留在该断点处，此时你可以使用截图、脚本录制（只能在 .macro 中进行录制），或自行获取 selector。比如

```
Scenario: 酷家乐网页登录测试-登录错误
	* input "12345" to "输入密码"
	* tap "登录"
	debugger
	* assert_exists "账号密码不正确"
```



***OK，以上就是所有你需要掌握的啦～，恭喜！*** 接下来你可以学习下 KoolTest 的基础词汇啦。[点击前往学习](https://github.com/Kujiale-Mobile/KoolTest/wiki/%E5%9F%BA%E7%A1%80%E8%AF%8D%E6%B1%87)



## 开源说明

本开源库为 KoolTest 的运行时部分，由 java 代码编写。Clone 本库后，可以使用 任何顺手的 Java IDE 进行修改调试。然后可以使用 `mvn package` 进行打包，得到的 jar 包包含了所有依赖，可以直接使用。 以下为该 jar 包的所有参数。

```
java -jar KoolTest.jar . // 其中 . 表示当前目录，该命令会执行在当前目录下的所有 .feature 文件
```

| 参数名                  | 默认值 | 说明                                                         |
| ----------------------- | ------ | ------------------------------------------------------------ |
| --name                  |        | 运行的 scenario 名，可以使用正则让多个 scenario 一起运行     |
| --tags                  |        | 运行某些 @Tag 的 Scenario，可以用 ~ 排除某些标签，比如 --tags "@SmokeTest,~@WIP"，则运行所有 @SmokeTest 标签的 Scenario，而不运行有 @WIP 标签的 |
| --platform              | chrome | 指定运行的平台，可以为 chrome、edge、safari、firefox、android、ios |
| --script                | .      | 运行脚本目录，默认为当前目录，主要与 config 配置有关，配置错误会导致 config 找不到 |
| --server                |        | 所连接的服务地址，app 测试时可以指定某个 appium 地址，chrome 测试可以指定 chromedriver 运行地址 |
| --chromeDriver          |        | chromedriver 所在地址，如果设置了则不需要再设置 --server，运行时会自动运行该 driver。 |
| --env                   |        | 环境变量，格式为 "key:value key1:value1"                     |
| --showProcess           |        | 无参数，该次是否显示执行过程，如 java -jar KoolTest.jar . --showProcess，则会以显示的方式执行本目录下的所有执行过程 |
| --app                   |        | app 测试专用，设置 apk 或 ipa 的路径                         |
| --udid                  |        | app 测试专用，指定设备号                                     |
| --logDir                | target | log 的输出地址                                               |
| --remote-debugging-port |        | 本次运行打开 chrome 的调试端口，并且结束后不关闭 chrome，后面加端口号，如 9222 |

后续我们会逐步开源我们的 vscode 插件和 KoolTest运行平台 部分代码。欢迎大家继续关注。



## Thanks

KoolTest 的完成离不开社区的支持，此处特别感谢以下两个组织：

https://github.com/cucumber

https://github.com/appium



## License

```
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
```

