package software.wings.delegatetasks.validation.capabilitycheck;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static io.harness.k8s.kubectl.Utils.executeCommand;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_PATH_PLACEHOLDER;

import com.google.inject.Inject;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import org.apache.commons.lang3.StringUtils;
import software.wings.delegatetasks.validation.capabilities.HelmInstallationCapability;
import software.wings.helpers.ext.helm.HelmCommandTemplateFactory;
import software.wings.helpers.ext.helm.HelmConstants;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

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
            .getHelmCommandTemplate(HelmCommandTemplateFactory.HelmCliCommandType.VERSION, HelmConstants.HelmVersion.V3)
            .replace(HELM_PATH_PLACEHOLDER, encloseWithQuotesIfNeeded(helmPath))
            .replace("${COMMAND_FLAGS}", StringUtils.EMPTY);
    return CapabilityResponse.builder()
        .validated(executeCommand(helmVersionCommand, 2))
        .delegateCapability(capability)
        .build();
  }
}
