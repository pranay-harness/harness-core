package io.harness.delegate.task.executioncapability;

import static java.lang.String.format;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.PcfAutoScalarParameters;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.PcfAutoScalarCapability;
import io.harness.exception.InvalidArgumentsException;
import io.harness.pcf.CfCliDelegateResolver;
import io.harness.pcf.PcfUtils;
import io.harness.pcf.model.CfCliVersion;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PcfAutoScalarCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Inject private CfCliDelegateResolver cfCliDelegateResolver;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    PcfAutoScalarCapability pcfAutoScalarCapability = (PcfAutoScalarCapability) delegateCapability;
    CfCliVersion cfCliVersion = pcfAutoScalarCapability.getVersion();
    String cfCliPathOnDelegate = getCfCliPathOnDelegate(cfCliVersion);

    try {
      boolean validated = PcfUtils.checkIfAppAutoscalarInstalled(cfCliPathOnDelegate, cfCliVersion);
      if (!validated) {
        log.warn(
            "Could not find App Autoscalar plugin installed. CF PLUGIN HOME Used: {}", PcfUtils.resolvePcfPluginHome());
      }
      return CapabilityResponse.builder().delegateCapability(pcfAutoScalarCapability).validated(validated).build();
    } catch (Exception e) {
      log.error("Failed to Validate App-Autoscalar Plugin installed");
      return CapabilityResponse.builder().delegateCapability(pcfAutoScalarCapability).validated(false).build();
    }
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.PCF_AUTO_SCALAR_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    CfCliVersion cfCliVersion = convertPcfCliProtoVersion(parameters.getPcfAutoScalarParameters().getCfCliVersion());
    String cfCliPathOnDelegate = getCfCliPathOnDelegate(cfCliVersion);

    try {
      boolean validated = PcfUtils.checkIfAppAutoscalarInstalled(cfCliPathOnDelegate, cfCliVersion);
      if (!validated) {
        log.warn(
            "Could not find App Autoscalar plugin installed. CF PLUGIN HOME Used: {}", PcfUtils.resolvePcfPluginHome());
      }
      return builder.permissionResult(validated ? PermissionResult.ALLOWED : PermissionResult.DENIED).build();
    } catch (Exception e) {
      log.error("Failed to Validate App-Autoscalar Plugin installed");
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
  }

  private String getCfCliPathOnDelegate(CfCliVersion cfCliVersion) {
    return cfCliDelegateResolver.getAvailableCfCliPathOnDelegate(cfCliVersion)
        .orElseThrow(()
                         -> new InvalidArgumentsException(format(
                             "Unable to find installed CF CLI on delegate, requested version: %s", cfCliVersion)));
  }

  private static CfCliVersion convertPcfCliProtoVersion(PcfAutoScalarParameters.CfCliVersion protoVersion) {
    switch (protoVersion) {
      case V6:
        return CfCliVersion.V6;
      case V7:
        return CfCliVersion.V7;
      default:
        throw new InvalidArgumentsException(format("Pcf CLI version not found, protoVersion: %s", protoVersion));
    }
  }
}
