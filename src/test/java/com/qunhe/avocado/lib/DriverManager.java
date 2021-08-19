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

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.awt.*;

/**
 * 存储 driver，driver 主要分三档次
 * IOSDriver / AndroidDriver  继承  AppiumDriver  继承  RemoteWebDriver
 *
 * @author CPPAlien
 */
public class DriverManager {
    private static volatile DriverManager sDriverManager;
    private RemoteWebDriver mWebDriver;
    private AppiumDriver<MobileElement> mMobileDriver;
    private IOSDriver<MobileElement> mIOSDriver;
    private AndroidDriver<MobileElement> mAndroidDriver;
    private Robot mRobot;

    public static DriverManager get() {
        if (sDriverManager == null) {
            synchronized (DriverManager.class) {
                if (sDriverManager == null) {
                    sDriverManager = new DriverManager();
                }
            }
        }
        return sDriverManager;
    }

    public void setIOSDriver(IOSDriver<MobileElement> driver) {
        mIOSDriver = driver;
        mMobileDriver = driver;
        mWebDriver = driver;
    }

    public void setAndroidDriver(AndroidDriver<MobileElement> driver) {
        mAndroidDriver = driver;
        mMobileDriver = driver;
        mWebDriver = driver;
    }

    public void setWebDriver(RemoteWebDriver mWebDriver) {
        this.mWebDriver = mWebDriver;
    }

    public RemoteWebDriver baseDriver() {
        return mWebDriver;
    }

    public AppiumDriver<MobileElement> mobileDriver() {
        return mMobileDriver;
    }

    public AndroidDriver<MobileElement> androidDriver() {
        return mAndroidDriver;
    }

    public IOSDriver<MobileElement> iOSDriver() {
        return mIOSDriver;
    }
}
