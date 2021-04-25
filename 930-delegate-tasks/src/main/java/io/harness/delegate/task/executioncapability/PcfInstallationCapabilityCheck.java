package io.harness.delegate.task.executioncapability;

import static java.lang.String.format;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.PcfInstallationParameters;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.PcfInstallationCapability;
import io.harness.exception.InvalidArgumentsException;
import io.harness.pcf.PcfCliDelegateResolver;
import io.harness.pcf.model.PcfCliVersion;

import com.google.inject.Inject;

public class PcfInstallationCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Inject private PcfCliDelegateResolver pcfCliDelegateResolver;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    PcfInstallationCapability capability = (PcfInstallationCapability) delegateCapability;
    return CapabilityResponse.builder()
        .validated(pcfCliDelegateResolver.isDelegateEligibleToExecuteCliCommand(capability.getVersion()))
        .delegateCapability(capability)
        .build();
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.PCF_INSTALLATION_PARAMETERS) {
      return builder.permissionResult(CapabilitySubjectPermission.PermissionResult.DENIED).build();
    }

    PcfCliVersion pcfCliVersion = convertPcfCliVersion(parameters.getPcfInstallationParameters().getPcfCliVersion());
    return builder
        .permissionResult(pcfCliDelegateResolver.isDelegateEligibleToExecuteCliCommand(pcfCliVersion)
                ? CapabilitySubjectPermission.PermissionResult.ALLOWED
                : CapabilitySubjectPermission.PermissionResult.DENIED)
        .build();
  }

  private static PcfCliVersion convertPcfCliVersion(PcfInstallationParameters.PcfCliVersion protoVersion) {
    switch (protoVersion) {
      case V6:
        return PcfCliVersion.V6;
      case V7:
        return PcfCliVersion.V7;
      default:
        throw new InvalidArgumentsException(format("Pcf CLI version not found, protoVersion: %s", protoVersion));
    }
  }
}
