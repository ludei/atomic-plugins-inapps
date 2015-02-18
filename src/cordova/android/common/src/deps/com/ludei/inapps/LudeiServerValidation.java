package com.ludei.inapps;


import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

import javax.net.ssl.X509TrustManager;
import javax.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

/**
 * Represents Ludei's server validation object (the managed mode).
 *
 * @author Imanol Fernández
 * @version 1.0
 */
public class LudeiServerValidation implements InAppService.ValidationHandler{

    private AbstractInAppService mService;
    private int mPlatformId;

    /**
     * Class Constructor specifying the service and the platform Id.
     *
     * @param service The service.
     * @param platformId An int.
     */
    public LudeiServerValidation(AbstractInAppService service, int platformId) {
        mService = service;
        mPlatformId = platformId;
    }

    protected static final String API_KEY = "quohToh1pieF7ohmUieile6Koodae9ak6L0EeteeYiedaor8iCh5oowa";
    //protected static final String API_URL = "https://cloud-nightly.ludei.com/api/v2/verify-purchases/";
    protected static final String API_URL = "https://cloud.ludei.com/api/v2/verify-purchases/";

    protected void finish(final InAppService.ValidationCompletion completion, final InAppService.Error error) {
        mService.dispatchCallback(new Runnable() {
            @Override
            public void run() {
                completion.finishValidation(error);
            }
        });
    }

    protected JSONObject inputStreamToJson(InputStream stream) throws Exception {
        BufferedReader streamReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder responseStrBuilder = new StringBuilder();

        String inputStr;
        while ((inputStr = streamReader.readLine()) != null)
            responseStrBuilder.append(inputStr);
        return new JSONObject(responseStrBuilder.toString());
    }

    @Override
    public void onValidate(final String receipt, final String productId, final InAppService.ValidationCompletion completion) {

        mService.runBackgroundTask(new Runnable() {
            @Override
            public void run() {

                HttpsURLConnection connection;
                try {
                    JSONObject jsonRequestBody = new JSONObject();
                    jsonRequestBody.put("os", mPlatformId);
                    jsonRequestBody.put("api_key", API_KEY);
                    jsonRequestBody.put("debug", false);
                    jsonRequestBody.put("bundleId", mService.getContext().getPackageName());
                    jsonRequestBody.put("data", receipt);

                    // configure the SSLContext with a TrustManager
                    SSLContext ctx = SSLContext.getInstance("TLS");
                    ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
                    SSLContext.setDefault(ctx);

                    URL url = new URL(API_URL);
                    HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
                    connection = (HttpsURLConnection)url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setHostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String arg0, SSLSession arg1) {
                            return true;
                        }
                    });
                    connection.setDoOutput(true);
                    byte[] requestData = jsonRequestBody.toString().getBytes("UTF-8");
                    connection.setRequestProperty("Content-Type",  "application/json");
                    connection.setRequestProperty("Content-Length", Integer.toString(requestData.length));
                    OutputStream output = null;
                    try {
                        output = connection.getOutputStream();
                        output.write(requestData);
                    }
                    finally {
                        if (output != null) {
                            output.close();
                        }
                    }

                    int statusCode = connection.getResponseCode();
                    if (statusCode != 200) {
                        finish(completion, new InAppService.Error(statusCode, "Ludei Server validation failed with HTTP status code: " + statusCode));
                        return;
                    }

                    JSONObject json = inputStreamToJson(connection.getInputStream());
                    int validationStatus = json.optInt("status", -1);
                    if (validationStatus != 0) {
                        String errorMessage = "Ludei Server validation failed with message: " + json.optString("errorMessage", "") + " (status: " + validationStatus + ")";
                        finish(completion, new InAppService.Error(validationStatus, errorMessage));
                        return;
                    }

                    JSONArray orders = json.optJSONArray("orders");
                    if (orders == null || orders.length() == 0) {
                        finish(completion, new InAppService.Error(0, "Ludei Server validation failed with empty orders response"));
                        return;
                    }

                    String pid = orders.getJSONObject(0).optString("productId", "");
                    if (pid.equals(productId)) {
                        finish(completion, null);
                    }
                    else {
                        finish(completion, new InAppService.Error(0, "Ludei Server validation failed because productId does not match"));
                    }


                }
                catch (Exception e) {
                    finish(completion, new InAppService.Error(0, "Ludei Server validation failed with exception: " + e.toString()));
                }

            }
        });
    }

    private static class DefaultTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

}
