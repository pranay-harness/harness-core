package io.harness.ccm.cluster.entities;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.annotation.StoreIn;
import io.harness.ccm.cluster.entities.K8sYaml.K8sYamlKeys;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.google.common.hash.Hashing;
import java.util.Base64;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
@Entity(value = "k8sYaml", noClassnameStored = true)

@CdIndex(name = "accountId_hash", fields = { @Field(K8sYamlKeys.accountId)
                                             , @Field(value = K8sYamlKeys.hash) })
@FieldNameConstants(innerTypeName = "K8sYamlKeys")
public final class K8sYaml implements PersistentEntity, UuidAware, CreatedAtAware, AccountAccess {
  @Id private String uuid;
  long createdAt;

  private String accountId;
  private String clusterId;
  private String uid;

  private String resourceVersion;
  private String yaml;

  @FdUniqueIndex @Setter(AccessLevel.NONE) private String hash;

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
