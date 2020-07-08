package com.zhou.demo.ssl.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.util.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * @ClassName PemClientCredentials
 * @Author JackZhou
 * @Date 2020/6/4  10:28
 * @Desc  使用openssl默认的pem证书
 *
 * TrustManagerFactory，它主要是用来导入自签名证书，用来验证来自服务器的连接。
 * KeyManagerFactory，当开启双向验证时，用来导入客户端的密钥对。
 * SSLContext，SSL 上下文，使用上面的两个类进行初始化，就是个上下文环境。
 * SSLSocketFactory ：通过sslContext得到
 * SSLSocketFactory可以包含TrustManagerFactory和KeyManagerFactory
 **/

@Slf4j
public class PemClientCredentials {

    private static final String CA_CRT_ALIAS = "caCert-cert";
    private static final String CRT_ALIAS = "cert";

    private TrustManagerFactory trustManagerFactory;
    private KeyManagerFactory keyManagerFactory;

    private String caCertContent;
    private String privateKeyContent;
    private String clientCertContent;

    public PemClientCredentials(String caCertContent, String privateKeyContent, String clientCertContent) {
        this.caCertContent = caCertContent;
        this.privateKeyContent = privateKeyContent;
        this.clientCertContent = clientCertContent;
    }

    public static class SSLParams {
        public SSLSocketFactory sSLSocketFactory;
        public X509TrustManager trustManager;
    }

    public SSLParams getSSLParams(){
        try {
            // 1、通过证书得到TrustManagerFactory
            createAndInitTrustManagerFactory();
            //2、如果客户端证书和私钥存在，得到KeyManagerFactory
            createAndInitKeyManagerFactory();
            //3、Okhttp取消了，单参数方法，返回多参数用于Okhttp初始化
            SSLParams sslParams = new SSLParams();
            //得到ssl上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            sslParams.trustManager = getX509TrustManager();
            sslParams.sSLSocketFactory = sslContext.getSocketFactory();
            return sslParams;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
      * @Author JackZhou
      * @Description  证书文件的标签说明不需要处理
      * @Date 2020/6/4 15:08
     **/
    private void createAndInitTrustManagerFactory() {

        if(StringUtils.isEmpty(caCertContent)){
            throw new IllegalArgumentException("服务端证书不可为空");
        }
        try {
            KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            caKeyStore.load(null, null);
            Certificate certificate = readCertFile(caCertContent);
            caKeyStore.setCertificateEntry(CA_CRT_ALIAS, certificate);
            //初始化trustManagerFactory
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(caKeyStore);
        } catch (Exception e) {
            log.error("初始化TrustManagerFactory失败,证书内容{}", caCertContent, e);
            throw new RuntimeException(e);
        }
    }

    private X509TrustManager getX509TrustManager(){
        X509TrustManager x509TrustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
        return x509TrustManager;
    }

    private void createAndInitKeyManagerFactory(){
        if(StringUtils.isEmpty(privateKeyContent) || StringUtils.isEmpty(clientCertContent)){
            return;
        }
        try{
            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            clientKeyStore.load(null, null);
            char[] passwordCharArray = "".toCharArray();

            Certificate certificate = readCertFile(clientCertContent);
            clientKeyStore.setCertificateEntry(CRT_ALIAS, certificate);
            clientKeyStore.setKeyEntry("private-key", readPrivateKeyFile(privateKeyContent), passwordCharArray, new Certificate[]{certificate});

            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(clientKeyStore, passwordCharArray);
        }catch (Exception e){
            log.error("初始化KeyManagerFactory失败", e);
            throw new RuntimeException(e);
        }
    }

    private static PrivateKey readPrivateKeyFile(String fileContent) throws Exception {
        RSAPrivateKey privateKey = null;
        if (fileContent != null && !fileContent.isEmpty()) {
            fileContent = fileContent.replace("-----BEGIN PRIVATE KEY-----\n", "")
                    .replace("-----BEGIN PRIVATE KEY-----\r\n", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.decodeBase64(fileContent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        }
        return privateKey;
    }


    private X509Certificate readCertFile(String fileContent) throws Exception {
        X509Certificate certificate = null;
        if (fileContent != null && !fileContent.trim().isEmpty()) {
            fileContent = fileContent.replace("-----BEGIN CERTIFICATE-----\n", "")
                    .replace("-----BEGIN CERTIFICATE-----\r\n", "")
                    .replace("-----END CERTIFICATE-----", "");
            byte[] decoded = Base64.decodeBase64(fileContent);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            certificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(decoded));
        }
        return certificate;
    }

}
