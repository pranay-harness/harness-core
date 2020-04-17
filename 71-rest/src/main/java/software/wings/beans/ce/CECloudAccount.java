package software.wings.beans.ce;

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
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.ce.CECloudAccount.CECloudAccountKeys;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceCloudAccount", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "no_dup", unique = true), fields = {
    @Field(CECloudAccountKeys.accountId)
    , @Field(CECloudAccountKeys.infraAccountId), @Field(CECloudAccountKeys.infraMasterAccountId)
  })
})
@FieldNameConstants(innerTypeName = "CECloudAccountKeys")
public class CECloudAccount implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  String accountId;
  String accountArn;
  String accountName;
  String infraAccountId;
  String infraMasterAccountId; // master account id
  String masterAccountSettingId; // setting id of ce connectors
  long createdAt;
  long lastUpdatedAt;
}
