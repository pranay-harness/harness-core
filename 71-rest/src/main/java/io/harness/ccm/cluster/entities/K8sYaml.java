package io.harness.ccm.cluster.entities;

import io.harness.annotation.StoreIn;
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

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
@Entity(value = "k8sYaml", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "K8sYamlKeys")
public class K8sYaml implements PersistentEntity, UuidAware, CreatedAtAware {
  @Id private String uuid;
  long createdAt;

  private String clusterId;
  private String uid;

  private String resourceVersion;
  private String yaml;
}
