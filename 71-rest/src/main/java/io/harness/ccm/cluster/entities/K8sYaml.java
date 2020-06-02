package io.harness.ccm.cluster.entities;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.hash.Hashing;

import io.harness.annotation.StoreIn;
import io.harness.ccm.cluster.entities.K8sYaml.K8sYamlKeys;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import java.util.Base64;

@Data
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

  private String accountId;
  private String clusterId;
  private String uid;

  private String resourceVersion;
  private String yaml;

  @Indexed(options = @IndexOptions(unique = true)) @Setter(AccessLevel.NONE) private String hash;

  @Builder(toBuilder = true)
  private K8sYaml(String accountId, String clusterId, String uid, String resourceVersion, String yaml) {
    this.accountId = accountId;
    this.clusterId = clusterId;
    this.uid = uid;
    this.resourceVersion = resourceVersion;
    this.yaml = yaml;
    this.hash = hash(accountId, clusterId, uid, yaml);
  }

  public static String hash(String accountId, String clusterId, String uid, String yaml) {
    return Base64.getEncoder().encodeToString(Hashing.sha1()
                                                  .newHasher()
                                                  .putString(accountId, UTF_8)
                                                  .putString(clusterId, UTF_8)
                                                  .putString(uid, UTF_8)
                                                  .putString(yaml, UTF_8)
                                                  .hash()
                                                  .asBytes());
  }
}
