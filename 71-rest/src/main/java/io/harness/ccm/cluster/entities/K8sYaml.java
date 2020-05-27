package io.harness.ccm.cluster.entities;

import io.harness.annotation.StoreIn;
import io.harness.ccm.cluster.entities.K8sYaml.K8sYamlKeys;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
@Entity(value = "k8sYaml", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "accountId_uuid_resourceVersion", background = true),
      fields =
      {
        @Field(K8sYamlKeys.accountId)
        , @Field(K8sYamlKeys.uuid), @Field(value = K8sYamlKeys.resourceVersion, type = IndexType.DESC)
      })
  ,
      @Index(options = @IndexOptions(name = "accountId_clusterId_uid_resourceVersion", background = true), fields = {
        @Field(K8sYamlKeys.accountId)
        , @Field(K8sYamlKeys.clusterId), @Field(K8sYamlKeys.uuid),
            @Field(value = K8sYamlKeys.resourceVersion, type = IndexType.DESC)
      })
})
@FieldNameConstants(innerTypeName = "K8sYamlKeys")
public class K8sYaml implements PersistentEntity, UuidAware, CreatedAtAware, AccountAccess {
  @Id private String uuid;
  long createdAt;

  private String clusterId;
  private String uid;
  private String accountId;

  private String resourceVersion;
  private String yaml;
}
