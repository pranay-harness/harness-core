/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
