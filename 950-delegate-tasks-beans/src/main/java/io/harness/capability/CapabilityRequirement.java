package io.harness.capability;

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
public final class CapabilityRequirement implements PersistentEntity {
  // the account root for the capability requirement
  @FdIndex private String accountId;
  // the unique identifier for the capability
  @Id private String uuid;
  // Currently, capability permissions can only reside on delegates. However, if capabilites should
  // later be checked against other entities, we should specify which types of entities that the
  // capability should be checked against.

  /**
   * Amount of millis saying for how long the current capability check should be considered as valid. This amount
   * should be added to the system time after every capability re-validation and used to update the corresponding field
   * in
   * {@link io.harness.capability.CapabilitySubjectPermission}
   */
  private long maxValidityPeriod;

  /**
   * Amount of millis saying after how how much time the current capability check should be revalidated again. This
   * amount should be added to the system time after every capability re-validation and used to update the corresponding
   * field in {@link io.harness.capability.CapabilitySubjectPermission}
   */
  private long revalidateAfterPeriod;

  // when the capability was last used
  @FdTtlIndex private Date validUntil;

  // Queryable index of capability type
  private String capabilityType;

  // proto details about the capability
  private CapabilityParameters capabilityParameters;
}
