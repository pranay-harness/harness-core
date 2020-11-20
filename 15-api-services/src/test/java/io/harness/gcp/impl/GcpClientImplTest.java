package io.harness.gcp.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.container.Container;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
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
