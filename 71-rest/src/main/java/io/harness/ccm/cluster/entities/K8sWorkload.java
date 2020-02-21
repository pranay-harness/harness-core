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
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.PrePersist;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
@Entity(value = "k8sWorkload", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "no_dup_cluster", unique = true), fields = {
    @Field(K8sWorkloadKeys.clusterId), @Field(K8sWorkloadKeys.uid)
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

  // Mongo has problems for values having dot/period ('.') character. We replace dot with tilde
  // which is not an allowed k8s label character.
  @PrePersist
  void prePersist() {
    this.labels = Optional.ofNullable(labels)
                      .orElse(Collections.emptyMap())
                      .entrySet()
                      .stream()
                      .collect(Collectors.toMap(e -> encode(e.getKey()), e -> encode(e.getValue())));
  }

  @PostLoad
  void postLoad() {
    this.labels = Optional.ofNullable(labels)
                      .orElse(Collections.emptyMap())
                      .entrySet()
                      .stream()
                      .collect(Collectors.toMap(e -> decode(e.getKey()), e -> decode(e.getValue())));
  }

  private String encode(String decoded) {
    return decoded.replace('.', '~');
  }

  private String decode(String encoded) {
    return encoded.replace('~', '.');
  }
}
