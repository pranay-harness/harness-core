package io.harness.delegate.task.executioncapability;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.WinrmHostValidationParameters;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.CapabilityResponse.CapabilityResponseBuilder;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.WinrmHostValidationCapability;
import io.harness.winrm.WinRmChecker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WinrmHostValidationCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    WinrmHostValidationCapability capability = (WinrmHostValidationCapability) delegateCapability;
    CapabilityResponseBuilder capabilityResponseBuilder = CapabilityResponse.builder().delegateCapability(capability);
    log.info("Validating Winrm Session to Host: {}, Port: {}, useSsl: {}", capability.getHostname(),
        capability.getPort(), capability.isUseSSL());

    return capabilityResponseBuilder
        .validated(WinRmChecker.checkConnectivity(
            capability.getHostname(), capability.getPort(), capability.isUseSSL(), capability.getDomain()))
        .build();
  }

  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.WINRM_HOST_VALIDATION_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    WinrmHostValidationParameters capability = parameters.getWinrmHostValidationParameters();
    boolean validated = WinRmChecker.checkConnectivity(
        capability.getHostname(), capability.getPort(), capability.getUseSsl(), capability.getDomain());

    return builder.permissionResult(validated ? PermissionResult.ALLOWED : PermissionResult.DENIED).build();
  }
}
