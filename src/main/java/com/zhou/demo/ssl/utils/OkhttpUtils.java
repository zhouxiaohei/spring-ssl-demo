package com.zhou.demo.ssl.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName SimpleOkhttpUtils
 * @Author JackZhou
 **/
@Slf4j
public class OkhttpUtils {

   private static OkHttpClient okHttpClient = new OkHttpClient.Builder().readTimeout(6, TimeUnit.SECONDS).build();

   private static final MediaType JSON = MediaType.parse("application/json");

   private static final String MEDIATYPE_NONE = "none";
   private static final String MEDIATYPE_JSON = "application/json";
   private static final String MEDIATYPE_FORM = "form-data";
   private static final String MEDIATYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";

   public static OkHttpClient getInstance(){
       return okHttpClient;
   }

    /**
      * @Author JackZhou
      * @Description  执行get请求
     **/
    public static String execRequest(String url, Map<String, String> headers, OkHttpClient execClient){

        if(execClient == null){
            execClient = okHttpClient;
        }
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if(headers != null && headers.size() >0 ){
            headers.entrySet().stream().forEach(entry -> requestBuilder.header(entry.getKey(), entry.getValue()));
        }
        Request request = requestBuilder.build();
        try {
            Response response = execClient.newCall(request).execute();
            return  response.body().string();
        } catch (IOException e) {
            log.info("执行http请求出错,地址:{}", url, e);
            return null;
        }
    }

    /**
     * @Author JackZhou
     * @Description  执行post请求
     **/
    public static String execPostRequest(String url, Map<String, String> headers, RequestBody requestBody, OkHttpClient execClient){
        if(execClient == null){
            execClient = okHttpClient;
        }
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if(headers != null && headers.size() >0 ){
            headers.entrySet().stream().forEach(entry -> requestBuilder.header(entry.getKey(), entry.getValue()));
        }
        Request request = requestBuilder.post(requestBody).build();
        try {
            Response response = execClient.newCall(request).execute();
            return  response.body().string();
        } catch (IOException e) {
            log.info("执行http请求出错,地址:{}", url, e);
            return null;
        }
    }

    /**
     * @Author JackZhou
     * @Description  得到post请求的RequestBody
     **/
    public static RequestBody getBody(String type, Map<String, String> formParam, String body){
        switch (type) {
            case MEDIATYPE_JSON:
                if(StringUtils.isEmpty(body)){
                    RequestBody.create(null, "");
                }
                return RequestBody.create(JSON, body);
            case MEDIATYPE_FORM:
                MultipartBody.Builder formBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                if(formParam != null && formParam.size() >0 ){
                    formParam.entrySet().stream().forEach(entry -> formBuilder.addFormDataPart(entry.getKey(), entry.getValue()));
                }
                return formBuilder.build();
            case MEDIATYPE_FORM_URLENCODED:
                FormBody.Builder builder = new FormBody.Builder();
                if(formParam != null && formParam.size() >0 ){
                    formParam.entrySet().stream().forEach(entry -> builder.add(entry.getKey(), entry.getValue()));
                }
                return builder.build();
            case MEDIATYPE_NONE:
                return RequestBody.create(null, "");
            default:
                throw new IllegalArgumentException("不支持的mediaType：" + type);
        }
    }

    /**
     * @Author JackZhou
     * @Description  得到拼接后的url  @RequestParam参数
     **/
    public static String getUrl(String url, Map<String, String> params){
        HttpUrl.Builder builder = HttpUrl.parse(url).newBuilder();
        if(params != null && params.size() > 0){
            params.entrySet().stream().forEach(entry -> builder.addQueryParameter(entry.getKey(), entry.getValue()));
        }
        return builder.build().toString();
    }

    public static void testOneWayAllHttps(String url){
        try {
            // 方式1  过期
//            OkHttpClient client = okHttpClient.newBuilder().
//                    sslSocketFactory(ClientCredentials.createSSLSocketFactory())
//                    .hostnameVerifier( (a,b) -> true).build();  // 校验hostname，返回true
            // 方式2
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustAllCerts trustAllCerts = new TrustAllCerts();
            sslContext.init(null, new TrustManager[]{trustAllCerts}, new SecureRandom());
            // sslContext初始化 TrustManager 为什么必须填  unable to find valid certification path to requested target
            //sslContext.init(null, null, new SecureRandom());
            OkHttpClient client = okHttpClient.newBuilder().
                    sslSocketFactory(sslContext.getSocketFactory(),  trustAllCerts)
                    .hostnameVerifier( (a,b) -> true).build();  // 校验hostname，返回true

            Request request = new Request.Builder().url(url).get().build();
            Response response = client.newCall(request).execute();
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public static void testOneWayHttps(){
        try {
            String caCertContent = FileUtils.readFile("src/main/resources/httpsClient/ca.crt");
            PemClientCredentials credentials = new PemClientCredentials(caCertContent, null, null);
            PemClientCredentials.SSLParams sslParams = credentials.getSSLParams();
            OkHttpClient client = okHttpClient.newBuilder().
                    sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                    .hostnameVerifier( (a,b) -> true).build();  // 校验hostname，返回true
            Request request = new Request.Builder().url("https://aa.test.com:11001/demo/bootswagger/person/123").get().build();
            Response response = client.newCall(request).execute();
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void testTwoWayHttps(){
        try {
            // ca和server 证书都行
            String caCertContent = FileUtils.readFile("src/main/resources/httpsClient/ca.crt");
            //String caCertContent = FileUtils.readFile("src/main/resources/httpsClient/server.crt");
            String clientCertContent = FileUtils.readFile("src/main/resources/httpsClient/client.crt");
            String clientKeyContent = FileUtils.readFile("src/main/resources/httpsClient/target_pkcs8_privatekey.key");
            //String clientKeyContent = FileUtils.readFile("src/main/resources/httpsClient/client.key");
            PemClientCredentials credentials = new PemClientCredentials(caCertContent, clientKeyContent, clientCertContent);
            PemClientCredentials.SSLParams sslParams = credentials.getSSLParams();
            OkHttpClient client = okHttpClient.newBuilder().
                    sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                    .hostnameVerifier( (a,b) -> true).build();  // 校验hostname，返回true
            Request request = new Request.Builder().url("https://aa.test.com:11001/demo/bootswagger/person/123").get().build();
            Response response = client.newCall(request).execute();
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // JKS双向验证方式成功
    public static void testTwoWayHttpsJks(){
        try {
            String caCertContent = FileUtils.readFile("src/main/resources/httpsClient/ca.crt");
            String clientJksfilePath = "src/main/resources/httpsClient/client.jks";
            JksClientCredentials jksClientCredentials = new JksClientCredentials(caCertContent, new FileInputStream(clientJksfilePath), "123456");
            JksClientCredentials.SSLParams sslParams = jksClientCredentials.getSSLParams();
            OkHttpClient client = okHttpClient.newBuilder().
                    sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                    .hostnameVerifier( (a,b) -> true).build();  // 校验hostname，返回true
            Request request = new Request.Builder().url("https://aa.test.com:11001/demo/bootswagger/person/123").get().build();
            Response response = client.newCall(request).execute();
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String url = "https://aa.test.com:11001/demo/bootswagger/person/123";
        //单向验证
        //testOneWayAllHttps("https://www.baidu.com/");
         //testOneWayAllHttps(url);
        //testOneWayHttps();

        // 双向验证
        //testTwoWayHttps();
        //testTwoWayHttpsJks();
    }
}