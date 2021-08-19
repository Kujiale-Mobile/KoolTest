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
package com.qunhe.avocado.lib.global;

/**
 * @author CPPAlien
 */
public class Platform {
    public static final String WEB = "web";
    public static final String IOS = "ios";
    public static final String ANDROID = "android";
    public static final String SAFARI = "safari";
    public static final String FIREFOX = "firefox";
    public static final String EDGE = "edge";
    public static final String IE = "ie";
    public static final String OPERA = "opera";

    public static String CURRENT_PLATFORM = WEB;
    public static String DEVICE_NAME;
    public static String UDID;
    public static String CHROME_DRIVER_PATH;
    //--remote-debugging-port=9222
    public static String CHROME_DEBUGGING;
    public static String PACKAGE_LOCATION;
    public static String SCRIPT_LOCATION;
    public static String SERVER_ADDRESS;
    // 全局showProcess
    public static Boolean SHOW_PROCESS = false;

    public static boolean isMobile() {
        return IOS.equals(CURRENT_PLATFORM) || ANDROID.equals(CURRENT_PLATFORM);
    }
}
