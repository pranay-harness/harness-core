package io.harness.ccm.cluster.entities;

import io.harness.annotation.StoreIn;
import io.harness.ccm.cluster.entities.K8sWorkload.K8sWorkloadKeys;
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
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Map;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
@Entity(value = "k8sWorkload", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "no_dup", unique = true), fields = {
    @Field(K8sWorkloadKeys.settingId), @Field(K8sWorkloadKeys.uid)
  })
})
@FieldNameConstants(innerTypeName = "K8sWorkloadKeys")
public class K8sWorkload implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  long createdAt;
  long lastUpdatedAt;

  @NotEmpty String accountId;
  @NotEmpty String clusterId;
  @NotEmpty String settingId;

  @NotEmpty String name;
  @NotEmpty String namespace;
  @NotEmpty String uid;
  @NotEmpty String kind;
  Map<String, String> labels;
}
