package io.harness.ccm.cluster.entities;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import software.wings.beans.entityinterface.AccountAccess;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ClusterRecordKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "clusterRecords", noClassnameStored = true)
public class ClusterRecord implements PersistentEntity, UuidAware, AccountAccess, CreatedAtAware {
  @Id String uuid;
  final String accountId;
  final Cluster cluster;
  @SchemaIgnore long createdAt;
}
