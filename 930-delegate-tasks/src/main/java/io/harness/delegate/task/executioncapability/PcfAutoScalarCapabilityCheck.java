package io.harness.delegate.task.executioncapability;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.PcfAutoScalarCapability;
import io.harness.pcf.PcfUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PcfAutoScalarCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    PcfAutoScalarCapability pcfAutoScalarCapability = (PcfAutoScalarCapability) delegateCapability;
    try {
      boolean validated = PcfUtils.checkIfAppAutoscalarInstalled();
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
    try {
      boolean validated = PcfUtils.checkIfAppAutoscalarInstalled();
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
}
