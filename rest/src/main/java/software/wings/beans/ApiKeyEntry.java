package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.jersey.JsonViews;

import java.util.List;

@Value
@Getter
@Entity(value = "apiKeys", noClassnameStored = true)
public class ApiKeyEntry extends Base {
  @Indexed @NotEmpty private String accountId;
  @JsonView(JsonViews.Internal.class) @NotEmpty private char[] encryptedKey;
  @JsonView(JsonViews.Internal.class) @NotEmpty private String hashOfKey;

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
