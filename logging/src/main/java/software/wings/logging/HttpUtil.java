package software.wings.logging;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by anubhaw on 5/2/17.
 */
public class HttpUtil {
  private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
  private static TrustManager[] trustAllCerts = getTrustManagers();
  private static SSLContext sc = getSslContext();

  public static boolean connectableHost(String host, int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), 5000); // 5 sec timeout
      return true;
    } catch (IOException ignored) {
    }
    return false; // Either timeout or unreachable or failed DNS lookup.
  }

  public static SSLContext getSslContext() {
    try {
      sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      logger.warn("Error while initializing the SSL context");
    }
    return sc;
  }

  static class SslTrustManager implements X509TrustManager {
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
      return new java.security.cert.X509Certificate[] {};
    }
    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
  }

  public static TrustManager[] getTrustManagers() {
    return new TrustManager[] {new SslTrustManager()};
  }

  public static OkHttpClient getUnsafeOkHttpClient() {
    try {
      return new OkHttpClient.Builder()
          .sslSocketFactory(HttpUtil.getSslContext().getSocketFactory())
          .hostnameVerifier((s, sslSession) -> true)
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(120, TimeUnit.SECONDS)
          .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
