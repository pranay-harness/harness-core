package io.harness.delegate.task.executioncapability;

import io.harness.capability.CapabilityParameters;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import lombok.Builder;
import lombok.Value;

import static io.harness.capability.CapabilitySubjectPermission.PermissionResult;

@Value
@Builder(toBuilder = true)
public class CapabilityCheckDetails {
  private String accountId;
  private String capabilityId;
  private String delegateId;
  private CapabilityType capabilityType;
  private CapabilityParameters capabilityParameters;
  private PermissionResult permissionResult;
}
