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
package com.qunhe.avocado.lib.model;

/**
 * @author CPPAlien
 */
public class Device {
    public static int screenWidth;
    public static int screenHeight;
    // 物理像素和逻辑像素的比例, iOS 设备上使用，Android 设备上默认为 1
    public static float WIDTH_RATIO = 1;
    public static float HEIGHT_RATIO = 1;
}
