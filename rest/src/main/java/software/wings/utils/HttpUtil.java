package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Splitter;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
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
      // Do nothing
    }
    return false; // Either timeout or unreachable or failed DNS lookup.
  }

  public static boolean connectableHost(String urlString) {
    try {
      URL url = new URL(urlString);
      return connectableHost(url.getHost(), url.getPort() < 0 ? 80 : url.getPort());
    } catch (MalformedURLException e) {
      logger.error("", e);
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
      HttpURLConnection connection =
          (HttpURLConnection) (shouldUseNonProxy(url) ? new URL(url).openConnection(Proxy.NO_PROXY)
                                                      : new URL(url).openConnection());
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

  public static OkHttpClient getUnsafeOkHttpClient(String url) {
    try {
      return getOkHttpClientBuilder()
          .sslSocketFactory(HttpUtil.getSslContext().getSocketFactory())
          .hostnameVerifier((s, sslSession) -> true)
          .connectTimeout(15000, TimeUnit.SECONDS)
          .proxy(checkAndGetNonProxyIfApplicable(url))
          .readTimeout(15000, TimeUnit.SECONDS)
          .build();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean validUrl(String url) {
    return urlValidator.isValid(url);
  }

  public static ServiceApiVersion parseApisVersion(String acceptHeader) {
    if (StringUtils.isEmpty(acceptHeader)) {
      return null;
    }

    String[] headers = acceptHeader.split(",");
    String header = headers[0].trim();
    if (!header.startsWith("application/")) {
      throw new IllegalArgumentException("Invalid header " + acceptHeader);
    }

    String versionHeader = header.replace("application/", "").trim();
    if (StringUtils.isEmpty(versionHeader)) {
      throw new IllegalArgumentException("Invalid header " + acceptHeader);
    }

    String[] versionSplit = versionHeader.split("\\+");

    String version = versionSplit[0].trim();
    if (version.toUpperCase().charAt(0) == 'V') {
      return ServiceApiVersion.valueOf(version.toUpperCase());
    }

    return ServiceApiVersion.values()[ServiceApiVersion.values().length - 1];
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

  public static HttpHost getHttpProxyHost(String url) {
    if (shouldUseNonProxy(url, System.getProperty("http.nonProxyHosts"))) {
      return null;
    }

    return getHttpProxyHost();
  }

  public static boolean shouldUseNonProxy(String url) {
    return shouldUseNonProxy(url, System.getProperty("http.nonProxyHosts"));
  }

  /**
   * Sets the HTTP proxy that will be used by connections created by this client. This takes precedence over
   * proxySelector, which is only honored when this proxy is null (which it is by default). To disable proxy use
   * completely, call setProxy(Proxy.NO_PROXY)
   */
  public static Proxy checkAndGetNonProxyIfApplicable(String url) {
    return shouldUseNonProxy(url) ? Proxy.NO_PROXY : null;
  }
  /**
   * as per Oracle doc,
   * http.nonProxyHosts:a list of hosts that should be reached directly, bypassing the proxy. This is a list of patterns
   * separated by '|'. The patterns may start or end with a '*' for wildcards. Any host matching one of these patterns
   * will be reached through a direct connection instead of through a proxy.
   *
   * Currently, we only support suffix format (and not prefix). e.g. localhost, *localhost
   * e.g. *localhost
   */
  public static boolean shouldUseNonProxy(String url, String nonProxyConfigString) {
    if (isNotBlank(url) && isNotBlank(nonProxyConfigString)) {
      String domain = getDomain(url);

      if (Splitter.on("|")
              .splitToList(nonProxyConfigString)
              .stream()
              .anyMatch(suffix -> checkPattern(suffix, domain))) {
        return true;
      }
    }
    return false;
  }

  public static String getDomain(String url) {
    String domain = url;
    if (domain.toLowerCase().startsWith("http://")) {
      domain = domain.substring(7);
    } else if (domain.toLowerCase().startsWith("https://")) {
      domain = domain.substring(8);
    }

    int index = domain.indexOf('/');
    if (index != -1) {
      domain = domain.substring(0, index);
    }

    index = domain.indexOf(':');
    if (index != -1) {
      domain = domain.substring(0, index);
    }

    return domain;
  }

  private static boolean checkPattern(String pattern, String domain) {
    if (pattern.charAt(0) == '*') { // remove *, *.jenkins.com to .jenkins.com
      pattern = pattern.substring(1);
    }

    return domain.toLowerCase().endsWith(pattern.toLowerCase());
  }

  public static OkHttpClient.Builder getOkHttpClientWithNoProxyValueSet(String url) {
    return getOkHttpClientBuilder().proxy(checkAndGetNonProxyIfApplicable(url));
  }

  public static OkHttpClient.Builder getOkHttpClientBuilder() {
    return new OkHttpClient.Builder().connectionPool(new ConnectionPool(0, 5, TimeUnit.MINUTES));
  }
}
