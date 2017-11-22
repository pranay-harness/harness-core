package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.jersey.JsonViews;
import software.wings.annotation.Encrypted;
import software.wings.annotation.Encryptable;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.ArtifactServerYaml;

/**
 * Created by peeyushaggarwal on 5/26/16.
 */
@JsonTypeName("JENKINS")
@Data
@Builder
@ToString(exclude = "password")
public class JenkinsConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Jenkins URL", required = true) @NotEmpty private String jenkinsUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @NotEmpty
  @Encrypted
  private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new jenkins config.
   */
  public JenkinsConfig() {
    super(SettingVariableTypes.JENKINS.name());
  }

  public JenkinsConfig(
      String jenkinsUrl, String username, char[] password, String accountId, String encryptedPassword) {
    super(SettingVariableTypes.JENKINS.name());
    this.jenkinsUrl = jenkinsUrl;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ArtifactServerYaml {
    public Yaml() {}

    public Yaml(String type, String name, String url, String username, String password) {
      super(type, name, url, username, password);
    }
  }
}
