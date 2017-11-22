package software.wings.beans.config;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.jersey.JsonViews;
import software.wings.annotation.Encrypted;
import software.wings.annotation.Encryptable;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.ArtifactServerYaml;

/**
 * Created by srinivas on 3/30/17.
 */
@JsonTypeName("NEXUS")
@Data
@Builder
public class NexusConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Nexus URL", required = true) @NotEmpty private String nexusUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @Encrypted
  @NotEmpty
  private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new Nexus config.
   */
  public NexusConfig() {
    super(SettingVariableTypes.NEXUS.name());
  }

  public NexusConfig(String nexusUrl, String username, char[] password, String accountId, String encryptedPassword) {
    this();
    this.nexusUrl = nexusUrl;
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
