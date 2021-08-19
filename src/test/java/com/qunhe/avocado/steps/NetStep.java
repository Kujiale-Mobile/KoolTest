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
package com.qunhe.avocado.steps;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.qunhe.avocado.lib.Avocado;
import com.qunhe.avocado.lib.constant.Settings;
import com.qunhe.avocado.lib.global.Global;
import com.qunhe.avocado.lib.global.Platform;
import com.qunhe.avocado.lib.util.Log;
import lombok.Data;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author CPPAlien
 */
public class NetStep {
    private final OkHttpClient client = trustAllSslClient(new OkHttpClient());

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public void doRequest(String method, String key, String url, String status, String json, boolean optional) throws Exception {
        try {
            url = Avocado.get().replaceVariables(url);
            json = Avocado.get().replaceVariables(json);
            Request.Builder builder = new Request.Builder()
                .url(url);
            if (json != null) {
                Matcher matcher = Pattern.compile("header:([^\n]+)").matcher(json);
                if (matcher.find()) {
                    String header = matcher.group(1);
                    LinkedTreeMap headerMap = new Gson().fromJson(header, LinkedTreeMap.class);
                    for (Object mapKey : headerMap.keySet()) {
                        builder.header((String) mapKey, (String) headerMap.get(mapKey));
                    }
                }
                Matcher bodyMatcher = Pattern.compile("body:([^\n]+)").matcher(json);
                String bodyString = "";
                if (bodyMatcher.find()) {
                    bodyString = bodyMatcher.group(1);
                }
                RequestBody body = RequestBody.create(bodyString, JSON);
                switch (method) {
                    case "PUT":
                        builder.put(body);
                        break;
                    case "POST":
                        builder.post(body);
                        break;
                    case "DELETE":
                        if (bodyString.isEmpty()) {
                            builder.delete();
                        } else {
                            builder.delete(body);
                        }
                }
            }
            doResponse(builder.build(), status, key, optional);
        } catch (Exception e) {
            if (!optional) {
                throw e;
            }
        }
    }

    private void doResponse(Request request, String status, String key, boolean optional) throws Exception {
        try (Response response = client.newCall(request).execute()) {
            if (status == null) {
                if (response.code() != 200 && !optional) {
                    Assert.fail(String.format("The http status code is %d, not 200", response.code()));
                }
            } else {
                int statusCode = Integer.parseInt(status);
                if (response.code() != statusCode && !optional) {
                    Assert.fail(String.format("The http status code is %d, not %d", response.code(), statusCode));
                }
            }
            ResponseBody responseBody = response.body();
            if (responseBody != null && key != null) {
                String bodyString = responseBody.string();
                Global.VARIABLES.put(key, new Gson().fromJson(bodyString, LinkedTreeMap.class));
            }
        }
    }

    public void screenshotAndCompare(String optional, String arg, Integer startX, Integer startY,
                                     Integer width, Integer height, Integer timeout) throws Exception {
        long realTimeOut = timeout == null ? Settings.TIMEOUT : timeout * 1000L;
        long startTime = System.currentTimeMillis();
        String desPic = Paths.get(Platform.SCRIPT_LOCATION, arg).toString();
        boolean result;
        String screenPath;
        do {
            screenPath = Avocado.get().getScreenPic();

            String clip = "";
            if (startX != null && startY != null && width != null && height != null) {
                clip = startX + "," + startY + "," + height + "," + width;
            }

            String host = this.getAssertEqualHost("kool_test_config", 1, "assert_equal_host");
            result = this.assertEqual(host, screenPath, desPic, clip);
        } while ((System.currentTimeMillis() - startTime <= realTimeOut) && !result);

        if (!result) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
            String dirPath = Paths.get(Settings.TARGET_DIR, Settings.RESULT_DIR, Avocado.get().getScenario().getName()
                    , df.format(System.currentTimeMillis())).toString();
            File dir = new File(dirPath);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.e("mkdir failed");
                    throw new IOException();
                }
            }

            File srcFile = new File(screenPath);
            FileUtils.copyToDirectory(srcFile, dir);

            Avocado.get().getScenario().embed(FileUtils.readFileToByteArray(new File(desPic)), "image/png");
            Avocado.get().getScenario().embed(FileUtils.readFileToByteArray(srcFile), "image/png");
        }
        if (optional == null) {
            Assert.assertTrue(result);
        }
    }

    public String getAssertEqualHost(String name, Integer stage, String key) throws IOException {
        Request request = new Request.Builder()
                .url("https://pub-cps.kujiale.com/cps/api/client/config?name=" + name + "&stage=" + stage + "&key=" + key)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
                Assert.fail(String.format("The http status code is %d, not 200", response.code()));
            } else {
                CmdString cmdString = new Gson().fromJson(Objects.requireNonNull(response.body()).string(), CmdString.class);
                if (Integer.parseInt(cmdString.getC()) == 0) {
                    return cmdString.getD();
                }
                Assert.fail(cmdString.getM());
            }
        }
        return "";
    }

    @Data
    private static class CmdBoolean {
        String c;
        Boolean d;
        String m;
    }

    @Data
    private static class CmdString {
        String c;
        String d;
        String m;
    }

    public Boolean assertEqual(String host, String path1, String path2, String clip) throws IOException {
        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("multipart/form-data; charset=utf-8");
        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("img1","1.png",
                        RequestBody.create(
                                new File(path1),
                                okhttp3.MediaType.parse("application/octet-stream")))
                .addFormDataPart("img2","2.png",
                        RequestBody.create(
                                new File(path2),
                                okhttp3.MediaType.parse("application/octet-stream")))
                .addFormDataPart("imgType","file");

        if (clip.length() > 0) {
            bodyBuilder.addFormDataPart("clip","0,0,100,100");
        }
        MultipartBody body = bodyBuilder.build();
        Request request = new Request.Builder()
                .url(host + "/assert_equal")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
                Assert.fail(String.format("The http status code is %d, not 200", response.code()));
            } else {
                CmdBoolean cmdBoolean = new Gson().fromJson(Objects.requireNonNull(response.body()).string(), CmdBoolean.class);
                return cmdBoolean.getD();
            }
        }
        return false;
    }

    private static final TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }
    };
    private static final SSLContext trustAllSslContext;
    static {
        try {
            trustAllSslContext = SSLContext.getInstance("SSL");
            trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
    private static final SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory();
    /*
     * This should not be used in production unless you really don't care
     * about the security. Use at your own risk.
     */
    private static OkHttpClient trustAllSslClient(OkHttpClient client) {
        OkHttpClient.Builder builder = client.newBuilder();
        builder.sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager)trustAllCerts[0]);
        builder.hostnameVerifier((hostname, session) -> true);
        return builder.build();
    }
}
