package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.annotation.Encrypted;
import io.harness.data.validator.Trimmed;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.ArtifactServerYaml;

@JsonTypeName("GIT")
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"sshSettingAttribute"})
@ToString(exclude = {"password", "sshSettingAttribute"})
public class GitConfig extends SettingValue implements EncryptableSetting {
  @Attributes(title = "Username", required = true) private String username;

  @Attributes(title = "Password", required = true) @Encrypted private char[] password;
  @NotEmpty @Attributes(title = "Git Repo Url", required = true) private String repoUrl;

  @Attributes(title = "Git Branch", required = true) private String branch;
  @SchemaIgnore private String reference;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;
  private String sshSettingId;
  @SchemaIgnore @Transient private SettingAttribute sshSettingAttribute;
  private boolean keyAuth;
  @Attributes(title = "Description") private String description;
  private String webhookToken;
  @SchemaIgnore @Transient private GitRepositoryType gitRepoType;
  @Transient private boolean generateWebhookUrl;

  @Trimmed private String authorName;
  @Trimmed @Email private String authorEmailId;

  public enum GitRepositoryType { YAML, TERRAFORM, TRIGGER }

  /**
   * Instantiates a new setting value.
   */
  public GitConfig() {
    super(SettingVariableTypes.GIT.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  @Builder
  public GitConfig(String username, char[] password, String repoUrl, String branch, String accountId,
      String encryptedPassword, String sshSettingId, SettingAttribute sshSettingAttribute, boolean keyAuth,
      String description, String webhookToken, String reference, boolean generateWebhookUrl) {
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
    this.description = description;
    this.generateWebhookUrl = generateWebhookUrl;
    this.webhookToken = webhookToken;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends ArtifactServerYaml {
    private String branch;
    private String reference;
    private String repoUrl;
    private String username;
    private String password;
    private boolean keyAuth;
    private String sshSettingId;
    private String description;
    private String webhookToken;

    @Builder
    public Yaml(String type, String harnessApiVersion, String url, String username, String password, String branch,
        String reference, UsageRestrictions usageRestrictions, boolean keyAuth, String sshSettingId,
        String description) {
      super(type, harnessApiVersion, url, username, password, usageRestrictions);
      this.branch = branch;
      this.reference = reference;
      this.keyAuth = keyAuth;
      this.sshSettingId = sshSettingId;
      this.description = description;
    }
  }
}
