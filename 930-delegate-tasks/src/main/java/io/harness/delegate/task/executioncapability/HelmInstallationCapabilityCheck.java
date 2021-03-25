package io.harness.delegate.task.executioncapability;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.helm.HelmConstants.HELM_PATH_PLACEHOLDER;
import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static io.harness.k8s.kubectl.Utils.executeCommand;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.HelmInstallationParameters;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HelmInstallationCapability;
import io.harness.helm.HelmCliCommandType;
import io.harness.helm.HelmCommandTemplateFactory;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.model.HelmVersion;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;

public class HelmInstallationCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Inject private K8sGlobalConfigService k8sGlobalConfigService;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    HelmInstallationCapability capability = (HelmInstallationCapability) delegateCapability;
    String helmPath = k8sGlobalConfigService.getHelmPath(capability.getVersion());
    if (hasNone(helmPath)) {
      return CapabilityResponse.builder().validated(false).delegateCapability(capability).build();
    }
    String helmVersionCommand =
        HelmCommandTemplateFactory.getHelmCommandTemplate(HelmCliCommandType.VERSION, HelmVersion.V3)
            .replace(HELM_PATH_PLACEHOLDER, encloseWithQuotesIfNeeded(helmPath))
            .replace("${COMMAND_FLAGS}", StringUtils.EMPTY);
    return CapabilityResponse.builder()
        .validated(executeCommand(helmVersionCommand, 2))
        .delegateCapability(capability)
        .build();
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.HELM_INSTALLATION_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    String helmPath = k8sGlobalConfigService.getHelmPath(
        convertHelmVersion(parameters.getHelmInstallationParameters().getHelmVersion()));
    if (hasNone(helmPath)) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    String helmVersionCommand =
        HelmCommandTemplateFactory.getHelmCommandTemplate(HelmCliCommandType.VERSION, HelmVersion.V3)
            .replace(HELM_PATH_PLACEHOLDER, encloseWithQuotesIfNeeded(helmPath))
            .replace("${COMMAND_FLAGS}", StringUtils.EMPTY);
    return builder
        .permissionResult(executeCommand(helmVersionCommand, 2) ? PermissionResult.ALLOWED : PermissionResult.DENIED)
        .build();
  }

  private static HelmVersion convertHelmVersion(HelmInstallationParameters.HelmVersion protoVersion) {
    switch (protoVersion) {
      case V2:
        return HelmVersion.V2;
      case V3:
        return HelmVersion.V3;
      default:
        throw new RuntimeException("Helm version not found");
    }
  }
}
