package software.wings.delegatetasks.validation.capabilitycheck;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.HelmInstallationCapability;
import io.harness.delegate.task.executioncapability.HelmInstallationCapabilityCheck;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.model.HelmVersion;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@TargetModule(Module._930_DELEGATE_TASKS)
public class HelmInstallationCapabilityCheckTest extends WingsBaseTest {
  @Mock private K8sGlobalConfigService k8sGlobalConfigService;
  @Inject @InjectMocks private HelmInstallationCapabilityCheck helmInstallationCapabilityCheck;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldPerformCapabilityCheck() {
    when(k8sGlobalConfigService.getHelmPath(HelmVersion.V2)).thenReturn("");
    CapabilityResponse capabilityResponse2 = helmInstallationCapabilityCheck.performCapabilityCheck(
        HelmInstallationCapability.builder().version(HelmVersion.V2).build());
    assertThat(capabilityResponse2).isNotNull();
    assertThat(capabilityResponse2.isValidated()).isFalse();
  }
}
