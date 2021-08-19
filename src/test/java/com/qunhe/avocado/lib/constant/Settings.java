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
package com.qunhe.avocado.lib.constant;

/**
 * @author CPPAlien
 */
public class Settings {
    public static String TARGET_DIR = "target";
    public static String SCREENSHOT_TMP_DIR = "tmp";
    public static String SCREENSHOT_DIR =  "screenshot";
    public static String RESULT_DIR = "result";
    public static final String CONFIG_PATH = "config/device.yml";
    public static final String NEW_ENV_FILE = "config/env.yml";
    public static final String Capabilities_File = "config/capabilities.yml";
    public static final String ELEMENT_PATH = "config/elements.csv";

    // 最大允许超时五分钟
    public final static long MAX_TIMEOUT = 5 * 60 * 1000;
    public final static long TIMEOUT = 15 * 1000;
    public final static long OPTIONAL_TIMEOUT = 5 * 1000;
    public static int FAIL_TRY_TIMES = 1;
}
