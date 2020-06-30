package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonView;
import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.ApiKeyEntry.ApiKeyEntryKeys;
import software.wings.beans.security.UserGroup;
import software.wings.jersey.JsonViews;

import java.util.List;

@Data
@Builder
@Entity(value = "apiKeys", noClassnameStored = true)
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "ApiKeyEntryKeys")
@CdUniqueIndex(
    name = "uniqueName", fields = { @Field(value = ApiKeyEntryKeys.accountId)
                                    , @Field(value = ApiKeyEntryKeys.name) })
public class ApiKeyEntry implements PersistentEntity, UuidAccess, CreatedAtAccess, AccountAccess {
  @Id private String uuid;
  @NotEmpty private String accountId;
  @NotEmpty private String name;
  private List<String> userGroupIds;
  private List<UserGroup> userGroups;
  @FdIndex private long createdAt;
  @JsonView(JsonViews.Internal.class) @NotEmpty private char[] encryptedKey;
  @Transient private String decryptedKey;
  @JsonView(JsonViews.Internal.class) @NotEmpty private String hashOfKey;
}
