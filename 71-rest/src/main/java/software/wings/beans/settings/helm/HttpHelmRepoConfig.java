package software.wings.beans.settings.helm;

import static io.harness.data.validator.EntityNameValidator.ALLOWED_CHARS_SERVICE_VARIABLE_MESSAGE;
import static io.harness.data.validator.EntityNameValidator.ALLOWED_CHARS_SERVICE_VARIABLE_STRING;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.data.validator.EntityName;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;

import java.util.Arrays;

@JsonTypeName("HTTP_HELM_REPO_CONFIG")
@Data
@Builder
@ToString(exclude = {"password"})
@EqualsAndHashCode(callSuper = false)
public class HttpHelmRepoConfig extends SettingValue implements HelmRepoConfig {
  @SchemaIgnore @NotEmpty private String accountId;

  @NotEmpty
  @EntityName(charSetString = ALLOWED_CHARS_SERVICE_VARIABLE_STRING, message = ALLOWED_CHARS_SERVICE_VARIABLE_MESSAGE)
  private String repoName;
  @NotEmpty private String chartRepoUrl;
  private String username;
  @Encrypted private char[] password;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  public HttpHelmRepoConfig() {
    super(SettingVariableTypes.HTTP_HELM_REPO_CONFIG.name());
  }

  public HttpHelmRepoConfig(String accountId, String repoName, String chartRepoUrl, String username,
      final char[] password, String encryptedPassword) {
    super(SettingVariableTypes.HTTP_HELM_REPO_CONFIG.name());
    this.accountId = accountId;
    this.repoName = repoName;
    this.chartRepoUrl = chartRepoUrl;
    this.username = username;
    this.password = Arrays.copyOf(password, password.length);
    this.encryptedPassword = encryptedPassword;
  }
}
