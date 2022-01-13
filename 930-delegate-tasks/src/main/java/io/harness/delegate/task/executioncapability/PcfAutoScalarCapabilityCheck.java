/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
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
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfCliVersion;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class PcfAutoScalarCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Inject private CfCliDelegateResolver cfCliDelegateResolver;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    PcfAutoScalarCapability pcfAutoScalarCapability = (PcfAutoScalarCapability) delegateCapability;
    CfCliVersion cfCliVersion = pcfAutoScalarCapability.getVersion();

    try {
      boolean validated = isAppAutoscalarInstalled(cfCliVersion);
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

    try {
      boolean validated = isAppAutoscalarInstalled(cfCliVersion);
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

  private boolean isAppAutoscalarInstalled(CfCliVersion cfCliVersion) throws PivotalClientApiException {
    Optional<String> cfCliPathOnDelegate = cfCliDelegateResolver.getAvailableCfCliPathOnDelegate(cfCliVersion);
    if (!cfCliPathOnDelegate.isPresent()) {
      return false;
    }

    return PcfUtils.checkIfAppAutoscalarInstalled(cfCliPathOnDelegate.get(), cfCliVersion);
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
