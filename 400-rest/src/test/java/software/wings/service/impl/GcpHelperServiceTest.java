/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.gcp.helpers.GcpHelperService;
import io.harness.exception.InvalidRequestException;
import io.harness.gcp.helpers.GcpCredentialsHelperService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.service.intfc.security.EncryptionService;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@OwnedBy(CDP)
public class GcpHelperServiceTest extends WingsBaseTest {
  @Mock private EncryptionService encryptionService;
  @Mock private GcpCredentialsHelperService gcpCredentialsHelperService;
  @Spy @InjectMocks private GcpHelperService gcpHelperService;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetGoogleCredentialWithEmptyFile() {
    GcpConfig gcpConfig = GcpConfig.builder().build();
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> gcpHelperService.getGoogleCredential(
                            gcpConfig.getServiceAccountKeyFileContent(), gcpConfig.isUseDelegateSelectors()))
        .withMessageContaining("Empty service key");

    gcpConfig.setServiceAccountKeyFileContent(new char[] {});
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> gcpHelperService.getGoogleCredential(
                            gcpConfig.getServiceAccountKeyFileContent(), gcpConfig.isUseDelegateSelectors()))
        .withMessageContaining("Empty service key");
  }

  @Test
  @Owner(developers = OwnerRule.AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnProxyConfiguredCredentials() throws IOException {
    System.setProperty("http.proxyHost", "proxyHost");
    GcpConfig gcpConfig = GcpConfig.builder().serviceAccountKeyFileContent(getServiceAccountKeyContent()).build();
    when(gcpCredentialsHelperService.getGoogleCredentialWithProxyConfiguredHttpTransport(
             gcpConfig.getServiceAccountKeyFileContent()))
        .thenReturn(new GoogleCredential());
    gcpHelperService.getGoogleCredential(
        gcpConfig.getServiceAccountKeyFileContent(), gcpConfig.isUseDelegateSelectors());
    verify(gcpCredentialsHelperService, only())
        .getGoogleCredentialWithProxyConfiguredHttpTransport(gcpConfig.getServiceAccountKeyFileContent());
    System.clearProperty("http.proxyHost");
  }

  @Test
  @Owner(developers = OwnerRule.AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnDefaultConfiguredCredentials() throws IOException {
    GcpConfig gcpConfig = GcpConfig.builder().serviceAccountKeyFileContent(getServiceAccountKeyContent()).build();
    when(gcpCredentialsHelperService.getGoogleCredentialWithProxyConfiguredHttpTransport(
             gcpConfig.getServiceAccountKeyFileContent()))
        .thenReturn(new GoogleCredential());
    gcpHelperService.getGoogleCredential(
        gcpConfig.getServiceAccountKeyFileContent(), gcpConfig.isUseDelegateSelectors());
    verify(gcpCredentialsHelperService, only())
        .getGoogleCredentialWithDefaultHttpTransport(gcpConfig.getServiceAccountKeyFileContent());
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void shouldThrowInvalidRequestExceptionIfBadKeyProvided() {
    GcpConfig gcpConfig =
        GcpConfig.builder().serviceAccountKeyFileContent("---PRIVATE-KEY-HERE---".toCharArray()).build();
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> gcpHelperService.getGoogleCredential(
                            gcpConfig.getServiceAccountKeyFileContent(), gcpConfig.isUseDelegateSelectors()))
        .withMessageContaining("Provided Service account key is not in JSON format");
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void shouldNotExposeKeyFileWhenBadKeyProvided() {
    GcpConfig gcpConfig =
        GcpConfig.builder().serviceAccountKeyFileContent("---PRIVATE-KEY-HERE---".toCharArray()).build();
    try {
      gcpHelperService.getGoogleCredential(
          gcpConfig.getServiceAccountKeyFileContent(), gcpConfig.isUseDelegateSelectors());
    } catch (Exception e) {
      boolean hasKeyInException = e.getMessage().contains("---PRIVATE-KEY-HERE---");
      assertThat(hasKeyInException).isFalse();
    }
  }

  private char[] getServiceAccountKeyContent() {
    String json = "{\n"
        + "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n"
        + "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n"
        + "  \"client_email\": \"test@test.com\",\n"
        + "  \"client_id\": \"test_client_id\",\n"
        + "  \"client_x509_cert_url\": \"test.com\",\n"
        + "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC/djm2k4VH+LjU\\nygyN3aFKD4flgcOJIRe/AQFbNs0ZpNq4D2yCvq3vyhlXPtOXgDS5lO42vau9nIaQ\\n4/F1fa4DD0ikB0nO2RnHR51moP8QoKG98qUFl1AvFQgWSa03+/nMNhNHx6iKYncR\\n77U3+wyEjQbRcSGckuvN0fNTWnj4VZMbvLO1I0/QN7ezhOT7Sdq4gqL1r430Y5CM\\n5CUw+mVv481iOIFkVKo0utBKQ8kivkD8ypmt6gv93uccRaXYhaKC5edkzx/cmgDG\\n/eIzzAnXyEvMLIyNcB7umSorPRFEjvGm+8QEMIneXnbatzfZGToYWhxYyPd0Ei1L\\nqyb0swRpAgMBAAECggEAbsNcnA03++0eMKw7Gw9uscPOYK3ziy+D51ITkMthWEaQ\\n6g6yPelGAwUuJ9UG4AyhdAN+1FhiNK2LA+N62ve8ZbS/13UH9AyQ50K9ApR9OwdS\\nyurHITbqgnJuXUZ+WXcCimt2N/5/uJ9CNzQv6JTqJvzXRu9l/IjathWR6VTbyv3l\\nH7on+SfmZst0Qzk/DI2kWz/LwW8fiHVQS4D+sTs3UrSZxSb6RvGpYaAyWA8esDiG\\nf2duGIXyt9zctOuo+F9XHPFxiMtQBmUT5Uh38tIAayMfGiG2VclywFGGIkKrUw5O\\n8a9Nb7DwhOQ8qKoHv6HBXY1/pCZY/xRA0y4A5uSMIQKBgQD2WYBrkwoTxS6F/7YS\\ngE8wf+no8jM5mPfkPM9MJJcWlA1s0uWZS7ovoE6js9Z7T5r2NPzPHRIYuW0VUSk7\\nT7zNL1w8oDJy1psYm0+DD/vfj2GxQd1g7PHKfLwEwStorjSLWT8Y9nEoirtDgiRP\\nW9iOVLj7hqjFtlpmdK8hlv/3tQKBgQDG9kkjzEQ1QmCWJHM8C2AaSWv+Z4F0YOXJ\\ncw/uVF29lKFJ7axIFbbEnWfd1EDGhEPONEwnZPDiXH+d87qzPa9vb1Nj6N19oCAx\\nY2I59Dl59rQFm2L6XefO0mPt0Z0218ifDG3E0Cb00jwVzl3jQHyi9s3+qE2dZptn\\nqmozmTdiZQKBgQDS6gIHGBbyokmYtDwATxZ9oaZ0qJiu4YarRFz/BfzNeeicmVu2\\nCZ3YlNl/UsN5Q8iarvcbo/oQbQE11Q0GGNi+m6POzCElLRQQ1zgWMCMnXqz3hDqz\\nd2n0QSMAtxohP7UA8WISTzzGxzBZNh08TJh2E5dk0f/BdsSjH5epnIx+WQKBgQCR\\nSB/KKwsXVNvQZtdaXfgT2c8o2o3V3DeOrR1R63rzxwcrQ4jMrkZiNYo1mhqemtAk\\nT13YWkXnFKH/RYzr+zwSg9kBmHW7mORJDAgax3H81B9KBNf2eAGfrVNYwfoppNGT\\nCfFRiKkZljZXufQmz9zy5oMu09iw+c66mN6pxNSDXQKBgAID3ZRqZcdig01OvnQL\\nnoYOJoB6l8urreyzrawCPxHiLV6wYF+Cf84LZsa6ZjwpfBJwhnlqvfI0VrXRcX4X\\nLCBZHwJ6RX2BOehkfKsZ1gmGf8mMAIF21zgP2wzuf1tUlyX4+AbnDYAApLB0D5Jm\\nvPL/DCz1adM9KR4rMa+WPAfZ\\n-----END PRIVATE KEY-----\",\n"
        + "  \"private_key_id\": \"private_key_id\",\n"
        + "  \"project_id\": \"test_project_id\",\n"
        + "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n"
        + "  \"type\": \"service_account\"\n"
        + "}\n";
    return json.toCharArray();
  }
}
