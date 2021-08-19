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

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.qunhe.avocado.lib.global.Platform;
import com.qunhe.avocado.lib.constant.Settings;
import com.qunhe.avocado.lib.model.Element;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author CPPAlien
 */
public class ElementUtil {
    private volatile static ElementUtil sUtil = null;
    private List<Element> mElements;
    private final Map<String, Element> mTempMap = new HashMap<>();

    public static ElementUtil instance() {
        if (sUtil == null) {
            synchronized (ElementUtil.class) {
                if (sUtil == null) {
                    sUtil = new ElementUtil();
                }
            }
        }
        return sUtil;
    }

    private ElementUtil() {
        try {
            Reader reader = Files.newBufferedReader(Paths.get(Platform.SCRIPT_LOCATION, Settings.ELEMENT_PATH));
            CsvToBean<Element> csvToBean =
                    new CsvToBeanBuilder<Element>(reader).withType(Element.class).withIgnoreLeadingWhiteSpace(true).build();
            mElements = csvToBean.parse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Element get(String name) {
        if (mTempMap.get(name) != null) {
            return mTempMap.get(name);
        }
        for (Element element : mElements) {
            if (element.getElementName().equals(name)) {
                mTempMap.put(name, element);
                return element;
            }
        }
        return null;
    }
}
