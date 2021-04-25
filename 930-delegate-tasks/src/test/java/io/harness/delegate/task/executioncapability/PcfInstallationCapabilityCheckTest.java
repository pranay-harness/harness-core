package io.harness.delegate.task.executioncapability;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.PcfInstallationCapability;
import io.harness.pcf.PcfCliDelegateResolver;
import io.harness.pcf.model.PcfCliVersion;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PcfInstallationCapabilityCheckTest extends CategoryTest {
  @Mock private PcfCliDelegateResolver pcfCliDelegateResolver;
  @InjectMocks private PcfInstallationCapabilityCheck pcfInstallationCapabilityCheck;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testPerformCapabilityCheck() {
    PcfInstallationCapability capability = PcfInstallationCapability.builder()
                                               .criteria("Pcf CLI version 6 is installed")
                                               .version(PcfCliVersion.V6)
                                               .build();
    doReturn(true).when(pcfCliDelegateResolver).isDelegateEligibleToExecuteCliCommand(PcfCliVersion.V6);

    CapabilityResponse capabilityResponse = pcfInstallationCapabilityCheck.performCapabilityCheck(capability);

    assertThat(capabilityResponse).isNotNull();
    assertThat(capabilityResponse.isValidated()).isTrue();
    assertThat(capability.getVersion()).isEqualTo(PcfCliVersion.V6);
    assertThat(capability.getCriteria()).isEqualTo("Pcf CLI version 6 is installed");
  }
}
