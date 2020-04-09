package software.wings.delegatetasks.validation.capabilitycheck;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.delegatetasks.validation.capabilities.HelmCommandCapability;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.HelmConstants;
import software.wings.helpers.ext.helm.HelmDeployService;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;

public class HelmCommandCapabilityCheckTest extends WingsBaseTest {
  @Inject @InjectMocks HelmCommandCapabilityCheck helmCommandCapabilityCheck;
  @Mock private HelmDeployService helmDeployService;
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void performCapabilityCheck() {
    HelmCommandRequest commandRequest =
        HelmInstallCommandRequest.builder().helmVersion(HelmConstants.HelmVersion.V3).build();
    HelmCommandResponse helmCommandResponse =
        new HelmCommandResponse(CommandExecutionResult.CommandExecutionStatus.SUCCESS, "");
    when(containerDeploymentDelegateHelper.createAndGetKubeConfigLocation(any())).thenReturn("kubeConfig");
    when(helmDeployService.ensureHelmInstalled(commandRequest)).thenReturn(helmCommandResponse);
    CapabilityResponse response = helmCommandCapabilityCheck.performCapabilityCheck(
        HelmCommandCapability.builder().commandRequest(commandRequest).build());
    assertThat(response).isNotNull();
    assertThat(response.isValidated()).isTrue();
  }
}