package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.jersey.JsonViews;

import java.util.List;

@Getter
@NoArgsConstructor
@Entity(value = "apiKeys", noClassnameStored = true)
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
public class ApiKeyEntry extends Base {
  @Indexed @NotEmpty private String accountId;
  @JsonView(JsonViews.Internal.class) @NotEmpty private char[] encryptedKey;
  @JsonView(JsonViews.Internal.class) @NotEmpty private String hashOfKey;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  @Builder
  public ApiKeyEntry(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, String accountId, char[] encryptedKey,
      String hashOfKey) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.accountId = accountId;
    this.encryptedKey = encryptedKey;
    this.hashOfKey = hashOfKey;
  }
}
