package software.wings.utils;

import static software.wings.utils.Util.isNotEmpty;

import okhttp3.OkHttpClient;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by anubhaw on 5/2/17.
 */
public class HttpUtil {
  private static UrlValidator urlValidator =
      new UrlValidator(new String[] {"http", "https"}, UrlValidator.ALLOW_LOCAL_URLS);
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

  public static boolean connectableHost(String urlString) {
    try {
      URL url = new URL(urlString);
      return connectableHost(url.getHost(), url.getPort() < 0 ? 80 : url.getPort());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return false;
  }

  public static boolean connectableHttpUrl(String url) {
    logger.info("Testing connectivity to url {}", url);
    // Create a trust manager that does not validate certificate chains
    try {
      // Install the all-trusting trust manager
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      // Create all-trusting host name verifier
      HostnameVerifier allHostsValid = (s, sslSession) -> true;
      // Install the all-trusting host verifier
      HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      connection.setRequestMethod(
          "GET"); // Changed to GET as some providers like artifactory SAAS is not accepting HEAD requests
      connection.setConnectTimeout(15000); // 20ms otherwise delegate times out
      connection.setReadTimeout(15000);
      int responseCode = connection.getResponseCode();
      if ((responseCode >= 200 && responseCode <= 399) || responseCode == 401 || responseCode == 403) {
        logger.info("Url {} is connectable", url);
        return true;
      }
    } catch (Exception e) {
      logger.info("Could not connect to url {}: {}", url, Misc.getMessage(e));
      return false;
    }
    return false;
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
          .connectTimeout(15000, TimeUnit.SECONDS)
          .readTimeout(15000, TimeUnit.SECONDS)
          .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean validUrl(String url) {
    return urlValidator.isValid(url);
  }

  public static HttpHost getHttpProxyHost() {
    String proxyHost = null;
    int proxyPort = -1;
    String proxyScheme = System.getProperty("proxyScheme");

    if ("http".equalsIgnoreCase(proxyScheme)) {
      String httpProxyHost = System.getProperty("http.proxyHost");
      String httpProxyPort = System.getProperty("http.proxyPort");
      if (isNotEmpty(httpProxyHost)) {
        proxyHost = httpProxyHost;
        if (isNotEmpty(httpProxyPort)) {
          proxyPort = Integer.valueOf(httpProxyPort);
        }
        proxyScheme = "http";
      }
    } else { // https by default
      String httpsProxyHost = System.getProperty("https.proxyHost");
      String httpsProxyPort = System.getProperty("https.proxyPort");
      if (isNotEmpty(httpsProxyHost)) {
        proxyHost = httpsProxyHost;
        if (isNotEmpty(httpsProxyHost)) {
          proxyPort = Integer.valueOf(httpsProxyPort);
        }
        proxyScheme = "https";
      }
    }
    return isNotEmpty(proxyHost) ? new HttpHost(proxyHost, proxyPort, proxyScheme) : null;
  }
}
