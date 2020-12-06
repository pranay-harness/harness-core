package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class GcpHelperServiceTest extends WingsBaseTest {
  @Mock private EncryptionService encryptionService;
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
}
