package software.wings.delegatetasks.validation.capabilitycheck;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.helm.HelmConstants.HELM_PATH_PLACEHOLDER;
import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static io.harness.k8s.kubectl.Utils.executeCommand;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import io.harness.helm.HelmCommandTemplateFactory;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.model.HelmVersion;

import software.wings.delegatetasks.validation.capabilities.HelmInstallationCapability;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;

public class HelmInstallationCapabilityCheck implements CapabilityCheck {
  @Inject private K8sGlobalConfigService k8sGlobalConfigService;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    HelmInstallationCapability capability = (HelmInstallationCapability) delegateCapability;
    String helmPath = k8sGlobalConfigService.getHelmPath(capability.getVersion());
    if (isEmpty(helmPath)) {
      return CapabilityResponse.builder().validated(false).delegateCapability(capability).build();
    }
    String helmVersionCommand =
        HelmCommandTemplateFactory
            .getHelmCommandTemplate(HelmCommandTemplateFactory.HelmCliCommandType.VERSION, HelmVersion.V3)
            .replace(HELM_PATH_PLACEHOLDER, encloseWithQuotesIfNeeded(helmPath))
            .replace("${COMMAND_FLAGS}", StringUtils.EMPTY);
    return CapabilityResponse.builder()
        .validated(executeCommand(helmVersionCommand, 2))
        .delegateCapability(capability)
        .build();
  }
}
