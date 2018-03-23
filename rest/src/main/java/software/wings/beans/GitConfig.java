package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.ArtifactServerYaml;

@JsonTypeName("GIT")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@ToString(exclude = "password")
public class GitConfig extends SettingValue implements Encryptable {
  @NotEmpty @Attributes(title = "Username", required = true) private String username;

  @Attributes(title = "Password", required = true) @Encrypted private char[] password;
  @NotEmpty @Attributes(title = "Git Repo Url", required = true) private String repoUrl;

  @NotEmpty @Attributes(title = "Git Branch", required = true) private String branch;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;
  /**
   * Instantiates a new setting value.
   */
  public GitConfig() {
    super(SettingVariableTypes.GIT.name());
  }

  public GitConfig(
      String username, char[] password, String repoUrl, String branch, String accountId, String encryptedPassword) {
    super(SettingVariableTypes.GIT.name());
    this.username = username;
    this.password = password;
    this.repoUrl = repoUrl;
    this.branch = branch;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends ArtifactServerYaml {
    private String branch;

    @Builder
    public Yaml(String type, String harnessApiVersion, String url, String username, String password, String branch,
        UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, url, username, password, usageRestrictions);
      this.branch = branch;
    }
  }
}
