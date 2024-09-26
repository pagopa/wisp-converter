package it.gov.pagopa.wispconverter.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProxyUtility {

    private static final TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {

        // Method to check client's trust - accepting all certificates
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
        }

        // Method to check server's trust - accepting all certificates
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
        }

        // Method to get accepted issuers - returning an empty array
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }};

    public static RestTemplate getProxiedClient(InetSocketAddress proxyAddress) throws NoSuchAlgorithmException, KeyManagementException {

        // Initialize SSL context with the defined trust managers
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, null);

        // Disable SSL verification for RestTemplate

        // Set the default SSL socket factory to use the custom SSL context
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

        // Set the default hostname verifier to allow all hostnames
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        // Create a RestTemplate with a custom request factory
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setProxy(new Proxy(Proxy.Type.HTTP, proxyAddress));

        return new RestTemplate(factory);
    }
}
