package software.wings.beans.ce;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.annotation.StoreIn;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import software.wings.beans.ce.CECluster.CEClusterKeys;

import com.google.common.hash.Hashing;
import java.util.Base64;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceCluster", noClassnameStored = true)
@NgUniqueIndex(name = "no_dup",
    fields =
    {
      @Field(CEClusterKeys.accountId)
      , @Field(CEClusterKeys.infraAccountId), @Field(CEClusterKeys.region), @Field(CEClusterKeys.clusterName)
    })
@FieldNameConstants(innerTypeName = "CEClusterKeys")
@StoreIn("events")
public final class CECluster implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  String accountId;
  String clusterName;
  String clusterArn;
  String region;
  String infraAccountId;
  String infraMasterAccountId;
  String parentAccountSettingId; // setting id of ce connectors
  @FdIndex String hash;
  long lastReceivedAt;
  long createdAt;
  long lastUpdatedAt;

  @Builder(toBuilder = true)
  private CECluster(String accountId, String clusterName, String clusterArn, String region, String infraAccountId,
      String infraMasterAccountId, String parentAccountSettingId) {
    this.accountId = accountId;
    this.clusterName = clusterName;
    this.clusterArn = clusterArn;
    this.region = region;
    this.infraAccountId = infraAccountId;
    this.infraMasterAccountId = infraMasterAccountId;
    this.parentAccountSettingId = parentAccountSettingId;
    this.hash = hash(accountId, clusterName, region, infraAccountId);
  }

  public static String hash(String accountId, String clusterName, String region, String infraAccountId) {
    return Base64.getEncoder().encodeToString(Hashing.sha1()
                                                  .newHasher()
                                                  .putString(accountId, UTF_8)
                                                  .putString(clusterName, UTF_8)
                                                  .putString(region, UTF_8)
                                                  .putString(infraAccountId, UTF_8)
                                                  .hash()
                                                  .asBytes());
  }
}
