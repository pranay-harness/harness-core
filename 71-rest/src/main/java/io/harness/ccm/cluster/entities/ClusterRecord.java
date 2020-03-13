package io.harness.ccm.cluster.entities;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ClusterRecordKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "clusterRecords", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class ClusterRecord implements PersistentEntity, UuidAware, AccountAccess, CreatedAtAware, UpdatedAtAware {
  @Id String uuid;
  @Indexed String accountId;
  final Cluster cluster;
  String[] perpetualTaskIds; // reference
  boolean isDeactivated;
  @SchemaIgnore long createdAt;
  long lastUpdatedAt;
}
