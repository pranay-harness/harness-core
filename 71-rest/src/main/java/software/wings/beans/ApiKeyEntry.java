package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonView;
import io.harness.annotation.HarnessEntity;
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
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.security.UserGroup;
import software.wings.jersey.JsonViews;

import java.util.List;

@Data
@Builder
@Entity(value = "apiKeys", noClassnameStored = true)
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "ApiKeyEntryKeys")
public class ApiKeyEntry implements PersistentEntity, UuidAccess, CreatedAtAccess, AccountAccess {
  @Id private String uuid;
  private String name;
  private List<String> userGroupIds;
  private List<UserGroup> userGroups;
  @Indexed private long createdAt;
  @Indexed @NotEmpty private String accountId;
  @JsonView(JsonViews.Internal.class) @NotEmpty private char[] encryptedKey;
  @Transient private String decryptedKey;
  @JsonView(JsonViews.Internal.class) @NotEmpty private String hashOfKey;
}
