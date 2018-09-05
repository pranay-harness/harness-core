package software.wings.beans;

import static software.wings.common.Constants.TOKEN_FIELD;
import static software.wings.common.Constants.USERNAME_PASSWORD_FIELD;
import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.beans.config.ArtifactSourceable;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.ArtifactServerYaml;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * Created by peeyushaggarwal on 5/26/16.
 */
@JsonTypeName("JENKINS")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = {"password", "token"})
public class JenkinsConfig extends SettingValue implements Encryptable, ArtifactSourceable {
  @Attributes(title = "Jenkins URL", required = true) @NotEmpty private String jenkinsUrl;
  @Attributes(title = "Authentication Mechanism", required = true, enums = {USERNAME_PASSWORD_FIELD, TOKEN_FIELD})
  @NotEmpty
  private String authMechanism;

  @Attributes(title = "Username") private String username;
  @Attributes(title = "Password/ API Token") @Encrypted private char[] password;
  @Attributes(title = "Bearer Token(HTTP Header)") @Encrypted private char[] token;
  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedToken;

  /**
   * Instantiates a new jenkins config.
   */
  public JenkinsConfig() {
    super(SettingVariableTypes.JENKINS.name());
    authMechanism = USERNAME_PASSWORD_FIELD;
  }

  @SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP2"})
  @Builder
  public JenkinsConfig(String jenkinsUrl, String username, char[] password, String accountId, String encryptedPassword,
      char[] token, String encryptedToken, String authMechanism) {
    super(SettingVariableTypes.JENKINS.name());
    this.jenkinsUrl = jenkinsUrl;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
    this.authMechanism = authMechanism;
    this.encryptedToken = encryptedToken;
    this.token = token;
  }

  @Override
  public String fetchUserName() {
    return getUsername();
  }

  @Override
  public String fetchRegistryUrl() {
    return getJenkinsUrl();
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends ArtifactServerYaml {
    private String token;
    private String authMechanism;

    @Builder
    public Yaml(String type, String harnessApiVersion, String url, String username, String password, String token,
        String authMechanism, UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, url, username, password, usageRestrictions);
      this.token = token;
      this.authMechanism = authMechanism;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class VerificationYaml extends VerificationProviderYaml {
    private String url;
    private String username;
    private String password = ENCRYPTED_VALUE_STR;
    private String token = ENCRYPTED_VALUE_STR;
    private String authMechanism;

    @Builder
    public VerificationYaml(String type, String harnessApiVersion, String url, String username, String password,
        String token, String authMechanism, UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.url = url;
      this.username = username;
      this.password = password;
      this.authMechanism = authMechanism;
      this.token = token;
    }
  }
}
