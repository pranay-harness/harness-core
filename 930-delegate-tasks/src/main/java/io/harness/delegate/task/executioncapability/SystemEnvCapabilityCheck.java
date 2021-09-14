/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.executioncapability;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.SystemEnvParameters;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SystemEnvCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    SystemEnvCheckerCapability systemEnvCheckerCapability = (SystemEnvCheckerCapability) delegateCapability;
    boolean valid = systemEnvCheckerCapability.getComparate().equals(
        System.getenv().get(systemEnvCheckerCapability.getSystemPropertyName()));
    return CapabilityResponse.builder().delegateCapability(systemEnvCheckerCapability).validated(valid).build();
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.SYSTEM_ENV_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    SystemEnvParameters systemEnvParameters = parameters.getSystemEnvParameters();
    return builder
        .permissionResult(
            systemEnvParameters.getComparate().equals(System.getenv().get(systemEnvParameters.getProperty()))
                ? PermissionResult.ALLOWED
                : PermissionResult.DENIED)
        .build();
  }
}
