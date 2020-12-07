package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.service.impl.gcp.GcpCredentialsHelperService;
import software.wings.service.intfc.security.EncryptionService;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class GcpHelperServiceTest extends WingsBaseTest {
  @Mock private EncryptionService encryptionService;
  @Mock private GcpCredentialsHelperService gcpCredentialsHelperService;
  @Spy @InjectMocks private GcpHelperService gcpHelperService;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetGoogleCredentialWithEmptyFile() throws IOException {
    GcpConfig gcpConfig = GcpConfig.builder().build();
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> gcpHelperService.getGoogleCredential(gcpConfig, new ArrayList<>(), false))
        .withMessageContaining("Empty service key");

    gcpConfig.setServiceAccountKeyFileContent(new char[] {});
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> gcpHelperService.getGoogleCredential(gcpConfig, new ArrayList<>(), false))
        .withMessageContaining("Empty service key");
  }

  @Test
  @Owner(developers = OwnerRule.AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnProxyConfiguredCredentials() throws IOException {
    System.setProperty("http.proxyHost", "proxyHost");
    GcpConfig gcpConfig = GcpConfig.builder().serviceAccountKeyFileContent("content".toCharArray()).build();
    when(gcpCredentialsHelperService.getGoogleCredentialWithProxyConfiguredHttpTransport(gcpConfig))
        .thenReturn(new GoogleCredential());
    gcpHelperService.getGoogleCredential(gcpConfig, new ArrayList<>(), false);
    verify(gcpCredentialsHelperService, only()).getGoogleCredentialWithProxyConfiguredHttpTransport(gcpConfig);
    System.clearProperty("http.proxyHost");
  }

  @Test
  @Owner(developers = OwnerRule.AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnDefaultConfiguredCredentials() throws IOException {
    GcpConfig gcpConfig = GcpConfig.builder().serviceAccountKeyFileContent("content".toCharArray()).build();
    when(gcpCredentialsHelperService.getGoogleCredentialWithProxyConfiguredHttpTransport(gcpConfig))
        .thenReturn(new GoogleCredential());
    gcpHelperService.getGoogleCredential(gcpConfig, new ArrayList<>(), false);
    verify(gcpCredentialsHelperService, only()).getGoogleCredentialWithDefaultHttpTransport(gcpConfig);
  }
}
