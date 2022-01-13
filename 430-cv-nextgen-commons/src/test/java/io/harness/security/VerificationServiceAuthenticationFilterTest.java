/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.security;

import static io.harness.beans.ClientType.PROMETHEUS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenCommonsTestBase;
import io.harness.beans.ClientType;
import io.harness.category.element.UnitTests;
import io.harness.entity.HarnessApiKey;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.nio.charset.Charset;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class VerificationServiceAuthenticationFilterTest extends CvNextGenCommonsTestBase {
  @Mock private ResourceInfo resourceInfo;
  @Mock private ContainerRequestContext containerRequestContext;
  @Inject private HPersistence hPersistence;
  @Inject private VerificationServiceAuthenticationFilter verificationServiceAuthenticationFilter;
  private String apiKey;
  private VerificationServiceAuthenticationFilter authenticationFilter;

  @Before
  public void setUp() {
    apiKey = generateUuid();
    hPersistence.save(HarnessApiKey.builder()
                          .clientType(PROMETHEUS)
                          .encryptedKey(EncryptionUtils.encrypt(apiKey.getBytes(Charset.forName("UTF-8")), null))
                          .build());
    authenticationFilter = spy(verificationServiceAuthenticationFilter);
    doReturn(false).when(authenticationFilter).publicAPI();
    doReturn(false).when(authenticationFilter).authenticationExemptedRequests(any(ContainerRequestContext.class));
    doReturn(true).when(authenticationFilter).isHarnessClientApi(any(ResourceInfo.class));
    doReturn(new ClientType[] {PROMETHEUS})
        .when(authenticationFilter)
        .getClientTypesFromHarnessApiKeyAuth(any(ResourceInfo.class));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testFilterForApiKeys() {
    when(containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(generateUuid());
    assertThatThrownBy(() -> authenticationFilter.filter(containerRequestContext)).isInstanceOf(WingsException.class);

    when(containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + apiKey);
    authenticationFilter.filter(containerRequestContext);
  }
}
