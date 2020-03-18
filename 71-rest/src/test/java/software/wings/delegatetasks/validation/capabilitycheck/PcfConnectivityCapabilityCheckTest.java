package software.wings.delegatetasks.validation.capabilitycheck;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.PcfConfig;
import software.wings.delegatetasks.validation.capabilities.PcfConnectivityCapability;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;

import java.util.ArrayList;

public class PcfConnectivityCapabilityCheckTest extends WingsBaseTest {
  private final PcfConfig pcfConfig =
      PcfConfig.builder().endpointUrl("pcfUrl").username(USER_NAME).password(PASSWORD).build();

  private final PcfConnectivityCapability pcfConnectivityCapability =
      PcfConnectivityCapability.builder().pcfConfig(pcfConfig).encryptionDetails(new ArrayList<>()).build();

  @Mock private PcfDeploymentManager pcfDeploymentManager;
  @Inject @InjectMocks private PcfConnectivityCapabilityCheck pcfConnectivityCapabilityCheck;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldPerformCapabilityCheck() {
    when(pcfDeploymentManager.checkConnectivity(pcfConfig)).thenReturn("SUCCESS");
    CapabilityResponse capabilityResponse =
        pcfConnectivityCapabilityCheck.performCapabilityCheck(pcfConnectivityCapability);
    assertThat(capabilityResponse).isNotNull();
    assertThat(capabilityResponse.isValidated()).isTrue();
  }
}