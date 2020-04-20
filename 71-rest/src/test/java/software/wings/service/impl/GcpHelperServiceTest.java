package software.wings.service.impl;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.ArrayList;

public class GcpHelperServiceTest extends WingsBaseTest {
  @Mock private EncryptionService encryptionService;
  @Spy @InjectMocks private GcpHelperService gcpHelperService;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetGoogleCredentialWithEmptyFile() throws IOException {
    GcpConfig gcpConfig = GcpConfig.builder().build();
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> gcpHelperService.getGoogleCredential(gcpConfig, new ArrayList<>()))
        .withMessageContaining("Empty service key");

    gcpConfig.setServiceAccountKeyFileContent(new char[] {});
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> gcpHelperService.getGoogleCredential(gcpConfig, new ArrayList<>()))
        .withMessageContaining("Empty service key");
  }
}