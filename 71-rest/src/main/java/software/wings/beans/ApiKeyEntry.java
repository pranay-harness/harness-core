package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonView;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.jersey.JsonViews;

@Value
@Builder
@Entity(value = "apiKeys", noClassnameStored = true)
@HarnessExportableEntity
public class ApiKeyEntry implements PersistentEntity, UuidAccess {
  public static final String ACCOUNT_ID_KEY = "accountId";

  @Id private String uuid;
  @Indexed @NotEmpty private String accountId;
  @JsonView(JsonViews.Internal.class) @NotEmpty private char[] encryptedKey;
  @JsonView(JsonViews.Internal.class) @NotEmpty private String hashOfKey;
}
