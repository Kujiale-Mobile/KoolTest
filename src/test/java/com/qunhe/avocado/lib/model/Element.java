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

import com.opencsv.bean.CsvBindByName;
import com.qunhe.avocado.lib.annotations.Mobile_Only;
import lombok.Data;

/**
 * @author CPPAlien
 */
@Data
public class Element {
    @CsvBindByName(column = "name")
    private String elementName;

    @CsvBindByName(column = "xpath")
    private String elementXPath;

    @CsvBindByName(column = "cssSelector")
    private String elementCssSelector;

    @CsvBindByName(column = "className")
    private String elementClassName;

    @CsvBindByName(column = "id")
    private String elementId;

    @Mobile_Only
    @CsvBindByName(column = "aId")
    private String accessibilityId;
}
