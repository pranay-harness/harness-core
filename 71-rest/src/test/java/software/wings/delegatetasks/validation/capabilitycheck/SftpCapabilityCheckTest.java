package software.wings.delegatetasks.validation.capabilitycheck;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.delegatetasks.validation.capabilities.SftpCapability;
import software.wings.service.impl.SftpHelperService;

public class SftpCapabilityCheckTest extends WingsBaseTest {
  private final SftpCapability sftpCapability = SftpCapability.builder().sftpUrl("sftp:\\\\10.0.0.1").build();

  @Mock private SftpHelperService sftpHelperService;
  @InjectMocks @Inject private SftpCapabilityCheck sftpCapabilityCheck;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestPerformCapabilityCheck() {
    Mockito.when(sftpHelperService.getSFTPConnectionHost(Matchers.any())).thenReturn("10.0.0.1");
    Mockito.when(sftpHelperService.isConnectibleSFTPServer(Matchers.any())).thenReturn(true);
    CapabilityResponse capabilityResponse = sftpCapabilityCheck.performCapabilityCheck(sftpCapability);
    assertThat(capabilityResponse).isNotNull();
    assertThat(capabilityResponse.isValidated()).isTrue();
  }
}