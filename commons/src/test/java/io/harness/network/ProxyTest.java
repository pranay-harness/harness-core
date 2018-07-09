package io.harness.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Properties;

public class ProxyTest {
  private String proxyHost;
  private int proxyPort;
  private String proxyUser;
  private String proxyPassword;
  private String invalidProxyPassword;
  private String targetUrl;

  @Before
  public void setup() throws Exception {
    Properties prop = new Properties();
    InputStream input = null;

    try {
      input = ProxyTest.class.getResourceAsStream("../../../testProxyconfig.properties");

      // load a properties file
      prop.load(input);

      // get the property value and print it out
      proxyHost = prop.getProperty("PROXY_HOST");
      proxyPort = Integer.parseInt(prop.getProperty("PROXY_PORT"));
      proxyUser = prop.getProperty("PROXY_USER");
      proxyPassword = prop.getProperty("PROXY_PASSWORD");
      targetUrl = prop.getProperty("TARGET_URL");
      invalidProxyPassword = prop.getProperty("INVALID_PROXY_PASSWORD");

    } finally {
      if (input != null) {
        input.close();
      }
    }
  }

  /**
   * No proxy configured
   * @throws IOException
   */
  @Test
  public void testWithNoProxyConfigured() throws IOException {
    OkHttpClient.Builder builder = new Builder();
    Request request1 = new Request.Builder().url(targetUrl).build();

    Response response = builder.build().newCall(request1).execute();
    assertEquals(200, response.code());

    Executor executor = Executor.newInstance();
    org.apache.http.client.fluent.Request request =
        org.apache.http.client.fluent.Request.Get(targetUrl).connectTimeout(10000).socketTimeout(10000);

    try {
      assertEquals("OK", executor.execute(request).returnResponse().getStatusLine().getReasonPhrase());
    } catch (Exception e) {
      assertTrue("Should not reach here", false);
    }
  }

  /**
   * Using proxy with Auth
   */
  @Test
  public void testWithProxyAuthSuccess() throws IOException {
    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
    OkHttpClient.Builder builder = new OkHttpClient.Builder().proxy(proxy);
    builder.proxyAuthenticator(new Authenticator() {
      public Request authenticate(Route route, Response response) throws IOException {
        String credential = Credentials.basic(proxyUser, proxyPassword);
        return response.request().newBuilder().header("Proxy-Authorization", credential).build();
      }
    });

    Request request = new Request.Builder().url(targetUrl).build();

    Response response = builder.build().newCall(request).execute();
    assertEquals(200, response.code());

    Executor executor = Executor.newInstance();
    org.apache.http.client.fluent.Request requestObj =
        org.apache.http.client.fluent.Request.Get(targetUrl).connectTimeout(10000).socketTimeout(10000);

    HttpHost httpHost = new HttpHost(proxyHost, proxyPort);
    requestObj.viaProxy(httpHost);
    executor.auth(httpHost, proxyUser, proxyPassword);

    try {
      String responsePhrase = executor.execute(requestObj).returnResponse().getStatusLine().getReasonPhrase();
      assertEquals("OK", responsePhrase);
    } catch (Exception e) {
      assertTrue("Should not reach here", false);
    }
  }

  /**
   * Proxy auth should fail due to invalid password
   * @throws IOException
   */
  @Test
  public void testWithProxyAuthFailWithInvalidCreds() throws IOException {
    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
    OkHttpClient.Builder builder = new OkHttpClient.Builder().proxy(proxy);

    builder.proxyAuthenticator(new Authenticator() {
      public Request authenticate(Route route, Response response) throws IOException {
        String credential = Credentials.basic(proxyUser, invalidProxyPassword);
        return response.request().newBuilder().header("Proxy-Authorization", credential).build();
      }
    });

    Request request = new Request.Builder().url(targetUrl).build();
    try {
      Response response = builder.build().newCall(request).execute();
      assertFalse("expected to fail auth", true);
    } catch (Exception ex) {
      assertTrue(true);
    }

    Executor executor = Executor.newInstance();
    org.apache.http.client.fluent.Request requestObj =
        org.apache.http.client.fluent.Request.Get(targetUrl).connectTimeout(10000).socketTimeout(10000);

    HttpHost httpHost = new HttpHost(proxyHost, proxyPort);
    requestObj.viaProxy(httpHost);
    executor.auth(httpHost, proxyUser, invalidProxyPassword);

    String responsePhrase = executor.execute(requestObj).returnResponse().getStatusLine().getReasonPhrase();
    assertEquals("Proxy Authentication Required", responsePhrase);
  }
}
