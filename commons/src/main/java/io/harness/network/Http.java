package io.harness.network;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Splitter;

import okhttp3.Authenticator;
import okhttp3.ConnectionPool;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class Http {
  private static UrlValidator urlValidator =
      new UrlValidator(new String[] {"http", "https"}, UrlValidator.ALLOW_LOCAL_URLS);
  private static final Logger logger = LoggerFactory.getLogger(Http.class);
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
      HttpURLConnection connection = getHttpsURLConnection(url);
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
      logger.info("Could not connect to url {}: {}", url, e.getMessage());
      return false;
    }
    return false;
  }

  private static HttpURLConnection getHttpsURLConnection(String url) throws MalformedURLException, IOException {
    HttpURLConnection connection = null;
    if (shouldUseNonProxy(url)) {
      connection = (HttpURLConnection) new URL(url).openConnection(Proxy.NO_PROXY);
    } else {
      connection = (HttpURLConnection) new URL(url).openConnection();
    }
    return connection;
  }

  private static boolean isProxyConfigured() {
    return isNotEmpty(getProxyHostName());
  }

  public static SSLContext getSslContext() {
    try {
      sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      logger.error("Error while initializing the SSL context", e);
    }
    return sc;
  }

  static class SslTrustManager implements X509TrustManager {
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
      return new java.security.cert.X509Certificate[] {};
    }

    @Override
    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
      // do nothing
    }

    @Override
    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
      // do nothing
    }
  }

  public static TrustManager[] getTrustManagers() {
    return new TrustManager[] {new SslTrustManager()};
  }

  public static synchronized OkHttpClient getUnsafeOkHttpClient(String url) {
    try {
      OkHttpClient.Builder builder = getOkHttpClientBuilder()
                                         .sslSocketFactory(Http.getSslContext().getSocketFactory())
                                         .hostnameVerifier((s, sslSession) -> true)
                                         .connectTimeout(15000, TimeUnit.SECONDS)
                                         .readTimeout(15000, TimeUnit.SECONDS);

      Proxy proxy = checkAndGetNonProxyIfApplicable(url);
      if (proxy != null) {
        builder.proxy(proxy);
      }

      return builder.build();

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
          proxyPort = Integer.parseInt(httpProxyPort);
        }
        proxyScheme = "http";
      }
    } else { // https by default
      String httpsProxyHost = System.getProperty("https.proxyHost");
      String httpsProxyPort = System.getProperty("https.proxyPort");
      if (isNotEmpty(httpsProxyHost)) {
        proxyHost = httpsProxyHost;
        if (isNotEmpty(httpsProxyHost)) {
          proxyPort = Integer.parseInt(httpsProxyPort);
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
    return getOkHttpClientWithProxyAuthSetup().connectionPool(new ConnectionPool(0, 5, TimeUnit.MINUTES));
  }

  public static OkHttpClient.Builder getOkHttpClientBuilderWithReadtimeOut(int timeout, TimeUnit timeUnit) {
    return getOkHttpClientBuilder().readTimeout(timeout, timeUnit);
  }

  public static OkHttpClient.Builder getOkHttpClientWithProxyAuthSetup() {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();

    String user = getProxyUserName();
    if (isNotEmpty(user)) {
      logger.info("###Using proxy Auth");
      String password = getProxyPassword();
      builder.proxyAuthenticator(new Authenticator() {
        public Request authenticate(Route route, Response response) throws IOException {
          String credential = Credentials.basic(user, password);
          return response.request().newBuilder().header("Proxy-Authorization", credential).build();
        }
      });
    }

    return builder;
  }

  public static String getProxyUserName() {
    String user = null;
    if ("http".equals(getProxyScheme().toLowerCase())) {
      user = System.getProperty("http.proxyUser");
    } else {
      user = System.getProperty("https.proxyUser");
    }
    if (isNotEmpty(user)) {
      return user;
    }

    return user;
  }

  // Need to add url encoding here
  public static String getProxyPassword() {
    String password = null;
    if (isNotEmpty(getProxyUserName())) {
      if ("http".equals(getProxyScheme().toLowerCase())) {
        password = System.getProperty("http.proxyPassword");
      } else {
        password = System.getProperty("https.proxyPassword");
      }
    }
    return password;
  }

  public static String getProxyScheme() {
    String proxyScheme = System.getProperty("proxyScheme");
    if (isNotEmpty(proxyScheme)) {
      return proxyScheme;
    }

    return "http";
  }

  // Need to add url encoding here
  public static String getProxyHostName() {
    String proxyHost = null;
    if ("http".equalsIgnoreCase(getProxyScheme())) {
      proxyHost = System.getProperty("http.proxyHost");
    } else { // https by default
      proxyHost = System.getProperty("https.proxyHost");
    }

    return proxyHost;
  }

  // Need to add url encoding here
  public static String getProxyPort() {
    String proxyPort = null;
    if ("http".equalsIgnoreCase(getProxyScheme())) {
      proxyPort = System.getProperty("http.proxyPort");
    } else { // https by default
      proxyPort = System.getProperty("https.proxyPort");
    }

    return proxyPort;
  }

  public static String getResponseFromUrl(String url, HttpHost httpProxyHost, int socketTimeout, int connectTimeout)
      throws IOException {
    Executor executor = Executor.newInstance();
    org.apache.http.client.fluent.Request request =
        org.apache.http.client.fluent.Request.Get(url).connectTimeout(connectTimeout).socketTimeout(socketTimeout);

    if (httpProxyHost != null) {
      if (!Http.shouldUseNonProxy(url)) {
        logger.info("using proxy");
        request.viaProxy(httpProxyHost);
        executor.auth(httpProxyHost, Http.getProxyUserName(), Http.getProxyPassword());
      }
    }

    org.apache.http.client.fluent.Response response = executor.execute(request);
    return response.returnContent().asString();
  }
}
