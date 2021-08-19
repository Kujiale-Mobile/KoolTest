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

import com.qunhe.avocado.lib.util.Log;
import org.openqa.selenium.OutputType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author CPPAlien
 */
public class ScreenShotType implements OutputType<File> {
    private final String mFileName;
    private final String mDir;

    ScreenShotType(String dir, String fileName) {
        this.mFileName = fileName;
        this.mDir = dir;
    }

    public File convertFromBase64Png(String base64Png) {
        return save(BYTES.convertFromBase64Png(base64Png));
    }

    public File convertFromPngBytes(byte[] data) {
        return save(data);
    }

    private File save(byte[] data) {
        OutputStream stream = null;

        try {
            File fd = new File(mDir);
            if (!fd.exists()) {
                boolean success = fd.mkdirs();
                if (!success) {
                    throw new IOException();
                }
            }

            File file = new File(fd.getPath(), mFileName + ".png");

            stream = new FileOutputStream(file);
            stream.write(data);

            return file;
        } catch (IOException e) {
            Log.w("save " + mDir + " " + mFileName + ".png failed");
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Nothing sane to do
                }
            }
        }
        return null;
    }

    public String toString() {
        return "OutputType.FILE";
    }
}
