package io.harness.delegate.task.executioncapability;

import static io.harness.capability.CapabilitySubjectPermission.PermissionResult;

import io.harness.delegate.beans.executioncapability.CapabilityType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class CapabilityCheckDetails {
  private String accountId;
  private String capabilityId;
  private String delegateId;
  private CapabilityType capabilityType;
  // TODO uncomment when class is visible here
  // private CapabilityParameters capabilityParameters;
  private PermissionResult permissionResult;
}
