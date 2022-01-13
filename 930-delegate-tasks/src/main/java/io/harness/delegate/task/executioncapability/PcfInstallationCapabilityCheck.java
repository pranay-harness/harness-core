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
import io.harness.capability.PcfInstallationParameters;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.PcfInstallationCapability;
import io.harness.exception.InvalidArgumentsException;
import io.harness.pcf.CfCliDelegateResolver;
import io.harness.pcf.model.CfCliVersion;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.CDP)
public class PcfInstallationCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Inject private CfCliDelegateResolver cfCliDelegateResolver;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    PcfInstallationCapability capability = (PcfInstallationCapability) delegateCapability;
    return CapabilityResponse.builder()
        .validated(cfCliDelegateResolver.isDelegateEligibleToExecuteCfCliCommand(capability.getVersion()))
        .delegateCapability(capability)
        .build();
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.PCF_INSTALLATION_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }

    CfCliVersion cfCliVersion = convertPcfCliProtoVersion(parameters.getPcfInstallationParameters().getCfCliVersion());
    return builder
        .permissionResult(cfCliDelegateResolver.isDelegateEligibleToExecuteCfCliCommand(cfCliVersion)
                ? PermissionResult.ALLOWED
                : PermissionResult.DENIED)
        .build();
  }

  private static CfCliVersion convertPcfCliProtoVersion(PcfInstallationParameters.CfCliVersion protoVersion) {
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
