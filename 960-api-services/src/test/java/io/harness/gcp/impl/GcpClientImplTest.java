/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gcp.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.container.Container;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class GcpClientImplTest extends CategoryTest {
  @Mock private GoogleCredential mockCredential;
  @Spy private GcpClientImpl gcpClient = new GcpClientImpl();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    doReturn(mockCredential).when(gcpClient).getDefaultGoogleCredentials();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getGkeContainerServiceSuccess() {
    Container gkeContainerService = gcpClient.getGkeContainerService();
    assertThat(gkeContainerService).isNotNull();
  }
}
