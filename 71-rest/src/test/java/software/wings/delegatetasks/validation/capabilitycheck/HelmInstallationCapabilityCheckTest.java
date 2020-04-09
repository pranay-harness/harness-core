package software.wings.delegatetasks.validation.capabilitycheck;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.delegatetasks.validation.capabilities.HelmInstallationCapability;
import software.wings.helpers.ext.helm.HelmConstants;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

public class HelmInstallationCapabilityCheckTest extends WingsBaseTest {
  @Mock private K8sGlobalConfigService k8sGlobalConfigService;
  @Inject @InjectMocks private HelmInstallationCapabilityCheck helmInstallationCapabilityCheck;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldPerformCapabilityCheck() {
    when(k8sGlobalConfigService.getHelmPath(HelmConstants.HelmVersion.V2)).thenReturn("");
    CapabilityResponse capabilityResponse2 = helmInstallationCapabilityCheck.performCapabilityCheck(
        HelmInstallationCapability.builder().version(HelmConstants.HelmVersion.V2).build());
    assertThat(capabilityResponse2).isNotNull();
    assertThat(capabilityResponse2.isValidated()).isFalse();
  }
}