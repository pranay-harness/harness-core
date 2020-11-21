package io.harness.capability;

import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import lombok.Builder;
import lombok.Data;

// The set of capability that is being used
@Data
@Builder
public final class CapabilitySubjectPermission implements PersistentEntity, UuidAware {
  private String accountId;
  private String capabilityId;
  // The only valid entity type is delegate right now
  private String uuid;
  // result will be considered stale at this time
  private long expirationTime;
  // Capability result: whether it is valid or not
  private enum PermissionResult { ALLOWED, DENIED }
  private PermissionResult subjectPermission;
  // If we need to add permission details, we should extend this.
}
