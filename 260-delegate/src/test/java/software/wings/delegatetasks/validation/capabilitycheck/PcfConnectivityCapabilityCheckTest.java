package software.wings.delegatetasks.validation.capabilitycheck;

import static io.harness.rule.OwnerRule.ANIL;
import static io.harness.rule.OwnerRule.TMACARI;

import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.USER_NAME_DECRYPTED;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.PcfConfig;
import software.wings.delegatetasks.validation.capabilities.PcfConnectivityCapability;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Spy;

@TargetModule(Module._930_DELEGATE_TASKS)
public class PcfConnectivityCapabilityCheckTest extends WingsBaseTest {
  private final PcfConfig pcfConfig =
      PcfConfig.builder().endpointUrl("pcfUrl").username(USER_NAME_DECRYPTED).password(PASSWORD).build();

  private final PcfConnectivityCapability pcfConnectivityCapability =
      PcfConnectivityCapability.builder().endpointUrl(pcfConfig.getEndpointUrl()).build();

  @Spy private PcfConnectivityCapabilityCheck pcfConnectivityCapabilityCheck;

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void shouldPerformCapabilityCheck() {
    doReturn(true)
        .when(pcfConnectivityCapabilityCheck)
        .isEndpointConnectable(eq(pcfConnectivityCapability), eq("https://"));
    doReturn(false)
        .when(pcfConnectivityCapabilityCheck)
        .isEndpointConnectable(eq(pcfConnectivityCapability), eq("http://"));
    CapabilityResponse capabilityResponse =
        pcfConnectivityCapabilityCheck.performCapabilityCheck(pcfConnectivityCapability);
    assertThat(capabilityResponse).isNotNull();
    assertThat(capabilityResponse.isValidated()).isTrue();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void shouldNotPassCapabilityCheck() {
    doReturn(false)
        .when(pcfConnectivityCapabilityCheck)
        .isEndpointConnectable(eq(pcfConnectivityCapability), eq("https://"));
    doReturn(false)
        .when(pcfConnectivityCapabilityCheck)
        .isEndpointConnectable(eq(pcfConnectivityCapability), eq("http://"));
    CapabilityResponse capabilityResponse =
        pcfConnectivityCapabilityCheck.performCapabilityCheck(pcfConnectivityCapability);
    assertThat(capabilityResponse).isNotNull();
    assertThat(capabilityResponse.isValidated()).isFalse();
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void validatePcfEndPointURL() {
    String pcfUrl = "api.pivotal.io";
    String expectedCapabilityUrl = "Pcf:" + pcfUrl;
    PcfConfig config = PcfConfig.builder().endpointUrl(pcfUrl).build();
    PcfConnectivityCapability capabilityCheck =
        PcfConnectivityCapability.builder().endpointUrl(config.getEndpointUrl()).build();
    String actualCapabilityUrl = capabilityCheck.fetchCapabilityBasis();

    // CDP-14589
    assertThat(actualCapabilityUrl).isEqualTo(expectedCapabilityUrl);

    // CDP-14738
    assertThat(pcfConfig.fetchRequiredExecutionCapabilities(null).size()).isEqualTo(0);
  }
}
