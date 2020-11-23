package io.harness.capability;

import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.google.common.collect.ImmutableList;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

// The set of capability that is being used
@Data
@Builder
@FieldNameConstants(innerTypeName = "CapabilitySubjectPermissionKeys")
@Entity(value = "capabilitySubjectPermission", noClassnameStored = true)
public final class CapabilitySubjectPermission implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("byDelegate")
                 .unique(true)
                 .field(CapabilitySubjectPermissionKeys.delegateId)
                 .field(CapabilitySubjectPermissionKeys.capabilityId)
                 .build())
        .build();
  }

  @FdIndex private String accountId;
  @FdIndex private String capabilityId;

  // The only valid entity type is delegate right now
  private String delegateId;

  // ID for individual entry
  @Id private String uuid;

  // result will be considered stale at this time
  @FdTtlIndex private Date validUntil;

  // Capability result: whether it is valid or not
  public enum PermissionResult { ALLOWED, DENIED, UNCHECKED }
  private PermissionResult permissionResult;
}
