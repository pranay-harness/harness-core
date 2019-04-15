package software.wings.beans.settings.helm;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;

import java.util.Arrays;

@JsonTypeName("HTTP_HELM_REPO")
@Data
@Builder
@ToString(exclude = {"password"})
@EqualsAndHashCode(callSuper = false)
public class HttpHelmRepoConfig extends SettingValue implements HelmRepoConfig {
  @SchemaIgnore @NotEmpty private String accountId;

  @NotEmpty private String chartRepoUrl;
  private String username;
  @Encrypted private char[] password;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  public HttpHelmRepoConfig() {
    super(SettingVariableTypes.HTTP_HELM_REPO.name());
  }

  public HttpHelmRepoConfig(
      String accountId, String chartRepoUrl, String username, final char[] password, String encryptedPassword) {
    super(SettingVariableTypes.HTTP_HELM_REPO.name());
    this.accountId = accountId;
    this.chartRepoUrl = chartRepoUrl;
    this.username = username;
    this.password = Arrays.copyOf(password, password.length);
    this.encryptedPassword = encryptedPassword;
  }

  @Override
  public String getConnectorId() {
    return null;
  }
}
