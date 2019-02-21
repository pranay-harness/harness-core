package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.annotation.NaturalKey;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.jersey.JsonViews;

@Getter
@NoArgsConstructor
@Entity(value = "apiKeys", noClassnameStored = true)
@HarnessExportableEntity
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
public class ApiKeyEntry extends Base {
  @Indexed @NotEmpty @NaturalKey private String accountId;
  @JsonView(JsonViews.Internal.class) @NotEmpty private char[] encryptedKey;
  @JsonView(JsonViews.Internal.class) @NotEmpty @NaturalKey private String hashOfKey;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  @Builder
  public ApiKeyEntry(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, String entityYamlPath, String accountId, char[] encryptedKey, String hashOfKey) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.accountId = accountId;
    this.encryptedKey = encryptedKey;
    this.hashOfKey = hashOfKey;
  }
}
