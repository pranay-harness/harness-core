package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.annotation.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.ArtifactServerYaml;

@JsonTypeName("GIT")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "password")
public class GitConfig extends SettingValue implements EncryptableSetting {
  @Attributes(title = "Username", required = true) private String username;

  @Attributes(title = "Password", required = true) @Encrypted private char[] password;
  @NotEmpty @Attributes(title = "Git Repo Url", required = true) private String repoUrl;

  @NotEmpty @Attributes(title = "Git Branch", required = true) private String branch;
  @SchemaIgnore private String reference;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;
  @SchemaIgnore private String sshSettingId;
  @SchemaIgnore @Transient private SettingAttribute sshSettingAttribute;
  @SchemaIgnore private boolean keyAuth;

  @SchemaIgnore @Transient private GitRepositoryType gitRepoType;

  public enum GitRepositoryType { YAML, TERRAFORM }

  /**
   * Instantiates a new setting value.
   */
  public GitConfig() {
    super(SettingVariableTypes.GIT.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  @Builder
  public GitConfig(String username, char[] password, String repoUrl, String branch, String reference, String accountId,
      String encryptedPassword, String sshSettingId, SettingAttribute sshSettingAttribute, boolean keyAuth) {
    super(SettingVariableTypes.GIT.name());
    this.username = username;
    this.password = password;
    this.repoUrl = repoUrl;
    this.branch = branch;
    this.reference = reference;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
    this.sshSettingId = sshSettingId;
    this.sshSettingAttribute = sshSettingAttribute;
    this.keyAuth = keyAuth;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends ArtifactServerYaml {
    private String branch;
    private String reference;

    @Builder
    public Yaml(String type, String harnessApiVersion, String url, String username, String password, String branch,
        String reference, UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, url, username, password, usageRestrictions);
      this.branch = branch;
      this.reference = reference;
    }
  }
}
