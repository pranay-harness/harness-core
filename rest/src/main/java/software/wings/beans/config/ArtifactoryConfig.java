package software.wings.beans.config;

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
 * Created by sgurubelli on 6/20/17.
 */
@JsonTypeName("ARTIFACTORY")
@Data
@ToString(exclude = "password")
@Builder
public class ArtifactoryConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Artifactory URL", required = true) @NotEmpty private String artifactoryUrl;

  @Attributes(title = "User Name") private String username;

  @JsonView(JsonViews.Internal.class) @Attributes(title = "Password") @Encrypted private char[] password;

  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedPassword;

  public ArtifactoryConfig() {
    super(SettingVariableTypes.ARTIFACTORY.name());
  }

  public ArtifactoryConfig(
      String artifactoryUrl, String username, char[] password, String accountId, String encryptedPassword) {
    this();
    this.artifactoryUrl = artifactoryUrl;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ArtifactServerYaml {
    public Yaml(String type, String name, String url, String username, String password) {
      super(type, name, url, username, password);
    }
  }
}
