package software.wings.beans.ce;

import io.harness.mongo.index.Field;
import io.harness.mongo.index.UniqueIndex;
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
import software.wings.beans.ce.CECluster.CEClusterKeys;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceCluster", noClassnameStored = true)
@UniqueIndex(name = "no_dup",
    fields =
    {
      @Field(CEClusterKeys.accountId)
      , @Field(CEClusterKeys.infraAccountId), @Field(CEClusterKeys.region), @Field(CEClusterKeys.clusterName)
    })
@FieldNameConstants(innerTypeName = "CEClusterKeys")
public class CECluster implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  String accountId;
  String clusterName;
  String region;
  String infraAccountId;
  String infraMasterAccountId;
  String parentAccountSettingId; // setting id of ce connectors
  String cloudProviderId; // setting id of this clusters cloud provider
  long lastReceivedAt;
  long createdAt;
  long lastUpdatedAt;
}
