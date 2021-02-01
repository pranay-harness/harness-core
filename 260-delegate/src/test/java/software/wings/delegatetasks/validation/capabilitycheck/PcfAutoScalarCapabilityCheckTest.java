package software.wings.delegatetasks.validation.capabilitycheck;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.pcf.PcfUtils;
import io.harness.pcf.PivotalClientApiException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.delegatetasks.validation.capabilities.PcfAutoScalarCapability;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PcfUtils.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
@TargetModule(Module._930_DELEGATE_TASKS)
public class PcfAutoScalarCapabilityCheckTest extends WingsBaseTest {
  @Inject @InjectMocks private PcfAutoScalarCapabilityCheck pcfAutoScalarCapabilityCheck;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldPerformCapabilityCheck() throws PivotalClientApiException {
    PowerMockito.mockStatic(PcfUtils.class);
    when(PcfUtils.checkIfAppAutoscalarInstalled()).thenReturn(true);
    CapabilityResponse capabilityResponse =
        pcfAutoScalarCapabilityCheck.performCapabilityCheck(PcfAutoScalarCapability.builder().build());
    assertThat(capabilityResponse).isNotNull();
    assertThat(capabilityResponse.isValidated()).isTrue();
  }
}
