/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration.network;

import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.harness.CategoryTest;
import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Properties;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ProxyTest extends CategoryTest {
  private String proxyHost;
  private int proxyPort;
  private String proxyHostNoAuth;
  private String proxyUser;
  private String proxyPassword;
  private String invalidProxyPassword;
  private String targetUrl;

  @Before
  public void setup() throws Exception {
    Properties prop = new Properties();
    InputStream input = null;

    try {
      input = ProxyTest.class.getResourceAsStream("../../../../testProxyconfig.properties");

      // load a properties file
      prop.load(input);

      // get the property value and print it out
      proxyHost = prop.getProperty("PROXY_HOST");
      proxyHostNoAuth = prop.getProperty("PROXY_HOST_NO_AUTH");
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
  @Owner(developers = ADWAIT)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testWithNoProxyConfigured_OkHttpClient() throws IOException {
    OkHttpClient.Builder builder = new Builder();
    Request request1 = new Request.Builder().url(targetUrl).build();

    Response response = builder.build().newCall(request1).execute();
    assertThat(response.code()).isEqualTo(200);

    Executor executor = Executor.newInstance();
    org.apache.http.client.fluent.Request request =
        org.apache.http.client.fluent.Request.Get(targetUrl).connectTimeout(10000).socketTimeout(10000);

    try {
      assertThat(executor.execute(request).returnResponse().getStatusLine().getReasonPhrase()).isEqualTo("OK");
    } catch (Exception e) {
      fail("Should not reach here");
    }
  }

  /**
   * Using proxy with Auth
   */

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testWithProxyAuthSuccess_OkHttpClient() throws IOException {
    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
    OkHttpClient.Builder builder = new OkHttpClient.Builder().proxy(proxy);
    builder.proxyAuthenticator(new Authenticator() {
      @Override
      public Request authenticate(Route route, Response response) throws IOException {
        String credential = Credentials.basic(proxyUser, proxyPassword);
        return response.request().newBuilder().header("Proxy-Authorization", credential).build();
      }
    });

    Request request = new Request.Builder().url(targetUrl).build();

    Response response = builder.build().newCall(request).execute();
    assertThat(response.code()).isEqualTo(200);

    Executor executor = Executor.newInstance();
    org.apache.http.client.fluent.Request requestObj =
        org.apache.http.client.fluent.Request.Get(targetUrl).connectTimeout(10000).socketTimeout(10000);

    HttpHost httpHost = new HttpHost(proxyHost, proxyPort);
    requestObj.viaProxy(httpHost);
    executor.auth(httpHost, proxyUser, proxyPassword);

    try {
      String responsePhrase = executor.execute(requestObj).returnResponse().getStatusLine().getReasonPhrase();
      assertThat(responsePhrase).isEqualTo("OK");
    } catch (Exception e) {
      fail("Should not reach here");
    }
  }

  /**
   * Proxy auth should fail due to invalid password
   * @throws IOException
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("ignoring Deprecated IntegrationTests for bazel")
  public void testWithProxyAuthFailWithInvalidCreds_OkHttpClient() throws IOException {
    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
    OkHttpClient.Builder builder = new OkHttpClient.Builder().proxy(proxy);

    builder.proxyAuthenticator(new Authenticator() {
      @Override
      public Request authenticate(Route route, Response response) throws IOException {
        String credential = Credentials.basic(proxyUser, invalidProxyPassword);
        return response.request().newBuilder().header("Proxy-Authorization", credential).build();
      }
    });

    Request request = new Request.Builder().url(targetUrl).build();
    try {
      Response response = builder.build().newCall(request).execute();
      fail("expected to fail auth");
    } catch (Exception ex) {
      // expected
    }
  }

  /**
   * Using proxy with Auth
   */
  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  public void testWithProxyWithNoAuthSuccess_OkHttpClient() throws IOException {
  //    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHostNoAuth, proxyPort));
  //    OkHttpClient.Builder builder = new OkHttpClient.Builder().proxy(proxy);
  //
  //    Request request = new Request.Builder().url(targetUrl).build();
  //    try {
  //      Response response = builder.build().newCall(request).execute();
  //      assertThat( response.code()).isEqualTo(200);
  //    } catch (Exception e) {
  //      assertThat(true).isFalse();
  //    }
  //
  //    builder = new OkHttpClient.Builder().proxy(proxy);
  //    builder.proxyAuthenticator(new Authenticator() {
  //      public Request authenticate(Route route, Response response) throws IOException {
  //        String credential = Credentials.basic(proxyUser, proxyPassword);
  //        return response.request().newBuilder().header("Proxy-Authorization", credential).build();
  //      }
  //    });
  //
  //    request = new Request.Builder().url(targetUrl).build();
  //    try {
  //      Response response = builder.build().newCall(request).execute();
  //      assertThat( response.code()).isEqualTo(200);
  //    } catch (Exception e) {
  //      assertThat(true).isFalse();
  //    }
  //  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testGetResponseFromUrlProxyAuth() throws IOException {
    Executor executor = Executor.newInstance();
    org.apache.http.client.fluent.Request request =
        org.apache.http.client.fluent.Request.Get(targetUrl).connectTimeout(1500).socketTimeout(1500);

    HttpHost httpProxyHost = new HttpHost(proxyHost, proxyPort, "http");
    request.viaProxy(httpProxyHost);
    // Add Auth if proxy auth is defined using System vars
    executor.auth(httpProxyHost, proxyUser, proxyPassword);
    org.apache.http.client.fluent.Response response = executor.execute(request);
    int responseCode = response.returnResponse().getStatusLine().getStatusCode();
    assertThat(responseCode).isEqualTo(200);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testGetResponseFromUrlProxyAuth_Fail() throws IOException {
    Executor executor = Executor.newInstance();
    org.apache.http.client.fluent.Request requestObj =
        org.apache.http.client.fluent.Request.Get(targetUrl).connectTimeout(10000).socketTimeout(10000);

    HttpHost httpHost = new HttpHost(proxyHost, proxyPort);
    requestObj.viaProxy(httpHost);
    executor.auth(httpHost, proxyUser, invalidProxyPassword);

    String responsePhrase = executor.execute(requestObj).returnResponse().getStatusLine().getReasonPhrase();
    assertThat(responsePhrase).isEqualTo("Proxy Authentication Required");
  }

  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  public void testGetResponseFromUrlProxyNoAuth() throws IOException {
  //    Executor executor = Executor.newInstance();
  //    org.apache.http.client.fluent.Request request =
  //        org.apache.http.client.fluent.Request.Get(targetUrl).connectTimeout(1500).socketTimeout(1500);
  //
  //    HttpHost httpProxyHost = new HttpHost(proxyHostNoAuth, proxyPort, "http");
  //    request.viaProxy(httpProxyHost);
  //    org.apache.http.client.fluent.Response response = executor.execute(request);
  //    int responseCode = response.returnResponse().getStatusLine().getStatusCode();
  //    assertThat( responseCode).isEqualTo(200);
  //  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("ignoring Deprecated IntegrationTests for bazel")
  public void testGetResponseFromUrlNoProxy() throws IOException {
    Executor executor = Executor.newInstance();
    org.apache.http.client.fluent.Request request =
        org.apache.http.client.fluent.Request.Get(targetUrl).connectTimeout(1500).socketTimeout(1500);

    org.apache.http.client.fluent.Response response = executor.execute(request);
    int responseCode = response.returnResponse().getStatusLine().getStatusCode();
    assertThat(responseCode).isEqualTo(200);
  }
}
