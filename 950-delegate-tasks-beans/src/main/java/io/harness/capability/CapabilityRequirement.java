package io.harness.capability;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.PersistentEntity;

import java.util.Date;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

// The capability that we are checking for
@Data
@Builder
@FieldNameConstants(innerTypeName = "CapabilityRequirementKeys")
@Entity(value = "capabilityRequirement", noClassnameStored = true)
@TargetModule(HarnessModule._460_CAPABILITY)
@StoreIn("dms")
public final class CapabilityRequirement implements PersistentEntity {
  // the account root for the capability requirement
  @FdIndex private String accountId;
  // the unique identifier for the capability
  @Id private String uuid;
  // Currently, capability permissions can only reside on delegates. However, if capabilites should
  // later be checked against other entities, we should specify which types of entities that the
  // capability should be checked against.

  // when the capability was last used
  @FdTtlIndex private Date validUntil;

  // Queryable index of capability type
  private String capabilityType;

  // proto details about the capability
  private CapabilityParameters capabilityParameters;
}
