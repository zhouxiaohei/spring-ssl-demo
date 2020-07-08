package com.zhou.demo.ssl.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * @ClassName JksClientCredentials
 * @Author JackZhou
 * @Date 2020/7/7  11:13
 **/

@Slf4j
public class JksClientCredentials {

    private TrustManagerFactory trustManagerFactory;
    private KeyManagerFactory keyManagerFactory;

    private static final String CA_CRT_ALIAS = "caCert-cert";

    private String caCertContent;
    private FileInputStream clientJksStream;
    private String keyStorePass;

    public JksClientCredentials(String caCertContent, FileInputStream clientJksStream, String keyStorePass) {
        this.caCertContent = caCertContent;
        this.clientJksStream = clientJksStream;
        this.keyStorePass = keyStorePass;
    }

    public static class SSLParams {
        public SSLSocketFactory sSLSocketFactory;
        public X509TrustManager trustManager;
    }

    public JksClientCredentials.SSLParams getSSLParams(){
        try {
            // 1、通过证书得到TrustManagerFactory
            createAndInitTrustManagerFactory();
            //2、如果客户端证书和私钥存在，得到KeyManagerFactory
            createAndInitKeyManagerFactory();
            //3、Okhttp取消了，单参数方法，返回多参数用于Okhttp初始化
            JksClientCredentials.SSLParams sslParams = new JksClientCredentials.SSLParams();
            //得到ssl上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
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
            Certificate certificate = getCertificate(caCertContent);
            caKeyStore.setCertificateEntry(CA_CRT_ALIAS, certificate);
            //初始化trustManagerFactory
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(caKeyStore);
        } catch (Exception e) {
            log.error("初始化TrustManagerFactory失败,证书内容{}", caCertContent, e);
            throw new RuntimeException(e);
        }
    }

    private void createAndInitKeyManagerFactory(){
        try{
            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            clientKeyStore.load(clientJksStream, keyStorePass.toCharArray());
            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(clientKeyStore, keyStorePass.toCharArray());
        }catch (Exception e){
            log.error("初始化KeyManagerFactory失败", e);
            throw new RuntimeException(e);
        }
    }

    private Certificate getCertificate(String content) throws CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Certificate certificate = certificateFactory.generateCertificate(new ByteArrayInputStream(content.getBytes()));
        return  certificate;
    }

    private X509TrustManager getX509TrustManager(){
        X509TrustManager x509TrustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
        return x509TrustManager;
    }

}
