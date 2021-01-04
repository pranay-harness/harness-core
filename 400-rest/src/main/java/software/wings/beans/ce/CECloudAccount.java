package software.wings.beans.ce;

import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.ce.CECloudAccount.CECloudAccountKeys;

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
@Entity(value = "ceCloudAccount", noClassnameStored = true)
@NgUniqueIndex(name = "no_dup_account",
    fields =
    {
      @Field(CECloudAccountKeys.accountId)
      , @Field(CECloudAccountKeys.infraAccountId), @Field(CECloudAccountKeys.infraMasterAccountId),
          @Field(CECloudAccountKeys.masterAccountSettingId)
    })
@FieldNameConstants(innerTypeName = "CECloudAccountKeys")
public class CECloudAccount implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  String accountId;
  String accountArn;
  String accountName;
  String infraAccountId;
  String infraMasterAccountId; // master account id
  AccountStatus accountStatus;
  String masterAccountSettingId; // setting id of ce connectors
  AwsCrossAccountAttributes awsCrossAccountAttributes;

  long createdAt;
  long lastUpdatedAt;

  public enum AccountStatus { NOT_VERIFIED, CONNECTED, NOT_CONNECTED }
}
