package io.harness.capability;

import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;

import com.google.protobuf.Any;
import lombok.Builder;
import lombok.Data;

// The capability that we are checking for
@Data
@Builder
public final class CapabilityRequirement implements PersistentEntity, UpdatedAtAware {
  // the account root for the capability requirement
  private String accountId;
  // the unique identifier for the capability
  private String capabilityId;
  // Currently, capability permissions can only reside on delegates. However, if capabilites should
  // later be checked against other entities, we should specify which types of entities that the
  // capability should be checked against.

  // how long a capability check should be valid for
  private long permissionTtl;
  // how long a capability should be checked before it is dropped from non-use
  private long capabilityTtl;
  // when the capability was last used
  private long lastUpdatedAt;
  // proto details about the capability
  private Any capabilityDetails;
}
