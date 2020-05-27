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
import lombok.NonNull;
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

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
@Entity(value = "k8sWorkload", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "no_dup_cluster", unique = true),
      fields = { @Field(K8sWorkloadKeys.clusterId)
                 , @Field(K8sWorkloadKeys.uid) })
  ,
      @Index(options = @IndexOptions(name = "accountId_clusterId_labels", background = true), fields = {
        @Field(K8sWorkloadKeys.accountId), @Field(K8sWorkloadKeys.clusterId), @Field(K8sWorkloadKeys.labels)
      }), @Index(options = @IndexOptions(name = "accountId_clusterId_uid", background = true), fields = {
        @Field(K8sWorkloadKeys.accountId), @Field(K8sWorkloadKeys.clusterId), @Field(K8sWorkloadKeys.uid)
      }), @Index(options = @IndexOptions(name = "accountId_name_clusterId_namespace", background = true), fields = {
        @Field(K8sWorkloadKeys.accountId)
        , @Field(K8sWorkloadKeys.name), @Field(K8sWorkloadKeys.clusterId), @Field(K8sWorkloadKeys.namespace)
      }), @Index(options = @IndexOptions(name = "accountId_name_labels", background = true), fields = {
        @Field(K8sWorkloadKeys.accountId), @Field(K8sWorkloadKeys.name), @Field(K8sWorkloadKeys.labels)
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
    this.labels = Optional.ofNullable(labels).map(K8sWorkload::encodeDotsInKey).orElse(null);
  }

  @PostLoad
  void postLoad() {
    this.labels = Optional.ofNullable(labels).map(K8sWorkload::decodeDotsInKey).orElse(null);
  }

  public static Map<String, String> encodeDotsInKey(@NonNull Map<String, String> labels) {
    return labels.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().replace('.', '~'), Map.Entry::getValue));
  }

  public static Map<String, String> decodeDotsInKey(@NonNull Map<String, String> labels) {
    return labels.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().replace('~', '.'), Map.Entry::getValue));
  }
}
