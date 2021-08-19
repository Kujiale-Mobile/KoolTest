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
package com.qunhe.avocado.lib.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author CPPAlien
 */
public class Log {
    private final static Logger LOGGER = Logger.getLogger("KoolTest");

    public static void e(String content) {
        LOGGER.log(Level.SEVERE, content);
    }

    public static void e(String content, Throwable throwable) {
        LOGGER.log(Level.SEVERE, content, throwable);
    }

    public static void w(String content) {
        LOGGER.log(Level.WARNING, content);
    }

    public static void i(String content) {
        LOGGER.log(Level.INFO, content);
    }
}
