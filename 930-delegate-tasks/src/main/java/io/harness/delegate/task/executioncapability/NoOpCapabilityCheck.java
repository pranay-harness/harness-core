/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

/**
 * This is temporary to facilitate perpetual task to honor selectors and get assigned to appropriate delegate.
 * Though execution of selector capability check task should be done by manager itself and shouldn't even reach delegate
 */
@OwnedBy(HarnessTeam.CE)
public class NoOpCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    return CapabilityResponse.builder().delegateCapability(delegateCapability).validated(true).build();
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    return CapabilitySubjectPermission.builder()
        .permissionResult(CapabilitySubjectPermission.PermissionResult.ALLOWED)
        .build();
  }
}
