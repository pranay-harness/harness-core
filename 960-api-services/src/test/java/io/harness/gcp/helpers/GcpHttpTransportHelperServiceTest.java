/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.gcp.helpers;

import static io.harness.rule.OwnerRule.AGORODETKI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GcpHttpTransportHelperServiceTest extends CategoryTest {
  private final GcpHttpTransportHelperService gcpHttpTransportHelperService = new GcpHttpTransportHelperService();

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnApacheHttpTransport() throws IOException, GeneralSecurityException {
    System.setProperty("http.proxyHost", "proxyHost");
    System.setProperty("http.proxyPort", "3218");
    HttpTransport httpTransport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
    assertThat(httpTransport).isInstanceOf(ApacheHttpTransport.class);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnNetHttpTransport() throws IOException, GeneralSecurityException {
    System.clearProperty("http.proxyHost");
    HttpTransport httpTransport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
    assertThat(httpTransport).isInstanceOf(NetHttpTransport.class);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnNetHttpTransportWhenGoogleApiHostIsInNonProxyList()
      throws IOException, GeneralSecurityException {
    System.setProperty("http.proxyHost", "proxyHost");
    System.setProperty("http.proxyPort", "3218");
    System.setProperty("http.nonProxyHosts", "googleapis.com");
    HttpTransport httpTransport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
    assertThat(httpTransport).isInstanceOf(NetHttpTransport.class);
  }
}
