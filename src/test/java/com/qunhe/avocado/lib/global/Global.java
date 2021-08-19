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

import java.util.HashMap;
import java.util.Map;

/**
 * 存储用于全局使用的变量
 * @author CPPAlien
 */
public class Global {
    // 存储一次测试中用户获得的变量
    public static volatile Map<String, Object> VARIABLES = new HashMap<>();
    public static volatile Map<String, Boolean> DEBUGGER_SCENARIOS = new HashMap<>();
}
