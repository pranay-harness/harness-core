/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
