/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.handler.impl.segment;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.event.handler.segment.SalesforceConfig;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SalesforceApiCheckTest extends WingsBaseTest {
  private SalesforceConfig salesforceConfig;
  private SalesforceApiCheck salesforceApiCheck;
  private HttpClient httpClient;
  private HttpResponse httpPostResponse;
  private HttpResponse httpGetResponse;
  private StatusLine statusLine;
  private HttpEntity httpEntity;
  private String loginUrl;

  private static final String OAUTH_ENDPOINT = "oauth_endpoint";
  private static final int CONNECTION_TIMEOUT = 1000;
  private static final int CONNECTION_REQUEST_TIMEOUT = 1000;
  private static final int SOCKET_TIMEOUT = 1000;

  @Before
  public void setUp() throws IllegalAccessException {
    salesforceConfig = initializeSalesforceConfig();
    salesforceApiCheck = spy(new SalesforceApiCheck(salesforceConfig));

    FieldUtils.writeField(salesforceApiCheck, "salesforceConfig", salesforceConfig, true);

    httpClient = Mockito.mock(HttpClient.class);
    FieldUtils.writeField(salesforceApiCheck, "httpClient", httpClient, true);

    loginUrl = getLoginUrl();
    FieldUtils.writeField(salesforceApiCheck, "loginUrl", loginUrl, true);

    RequestConfig requestConfig = RequestConfig.custom()
                                      .setConnectTimeout(CONNECTION_TIMEOUT)
                                      .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
                                      .setSocketTimeout(SOCKET_TIMEOUT)
                                      .build();

    httpPostResponse = Mockito.mock(HttpResponse.class);
    httpGetResponse = Mockito.mock(HttpResponse.class);
    statusLine = Mockito.mock(StatusLine.class);
  }

  private String getLoginUrl() {
    return "https://" + salesforceConfig.getLoginInstanceDomain() + OAUTH_ENDPOINT;
  }

  private SalesforceConfig initializeSalesforceConfig() {
    return SalesforceConfig.builder()
        .userName("user_name")
        .password("password")
        .consumerKey("consumer_key")
        .consumerSecret("consumer_secret")
        .grantType("grant_type")
        .apiVersion("api_version")
        .loginInstanceDomain("login_instance_domain")
        .build();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC0_testPresentInSalesforce() {
    Account account = getAccount(AccountType.PAID);
    boolean isPresent = salesforceApiCheck.isPresentInSalesforce(account);
    assertThat(isPresent).isFalse();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC1_testPresentInSalesforce() {
    Account account = getAccount(AccountType.PAID);
    account.setAccountName("PositiveAccountTesting");
    ProtocolVersion protocolVersion = new ProtocolVersion("protocol", 1, 2);
    statusLine = new BasicStatusLine(protocolVersion, 200, "reason_phrase");
    BasicHttpEntity entity = new BasicHttpEntity();
    entity.setContent(new ByteArrayInputStream(
        "{\"totalSize\" : \"0\", \"instance_url\" : \"instance_url\", \"access_token\" : \"access_token\"}"
            .getBytes()));
    entity.setContentLength(-1);
    entity.setChunked(true);

    httpGetResponse.setEntity(entity);
    httpPostResponse.setEntity(entity);
    boolean errorThrown = false;

    try {
      when(httpClient.execute(Mockito.any(HttpGet.class))).thenReturn(httpGetResponse);
      when(httpGetResponse.getStatusLine()).thenReturn(statusLine);
      when(httpGetResponse.getEntity()).thenReturn(entity);

      when(httpClient.execute(Mockito.any(HttpPost.class))).thenReturn(httpPostResponse);
      when(httpPostResponse.getStatusLine()).thenReturn(statusLine);
      when(httpPostResponse.getEntity()).thenReturn(entity);

      boolean isPresent = salesforceApiCheck.isPresentInSalesforce(account);
      assertThat(isPresent).isFalse();
    } catch (IOException ioe) {
      assertThat(ioe).isNotNull();
    }
  }
}
