/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.executioncapability;

import static io.harness.capability.CapabilitySubjectPermission.PermissionResult;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.capability.CapabilityParameters;
import io.harness.delegate.beans.executioncapability.CapabilityType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
public class CapabilityCheckDetails {
  private String accountId;
  private String capabilityId;
  private String delegateId;
  private CapabilityType capabilityType;
  private CapabilityParameters capabilityParameters;
  private PermissionResult permissionResult;
}
