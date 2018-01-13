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
import software.wings.yaml.setting.ArtifactServerYaml;

/**
 * Created by anubhaw on 1/5/17.
 */
@JsonTypeName("DOCKER")
@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "password")
public class DockerConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Docker Registry URL", required = true) @NotEmpty private String dockerRegistryUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @Encrypted
  private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new Docker registry config.
   */
  public DockerConfig() {
    super(SettingVariableTypes.DOCKER.name());
  }

  public DockerConfig(
      String dockerRegistryUrl, String username, char[] password, String accountId, String encryptedPassword) {
    super(SettingVariableTypes.DOCKER.name());
    this.dockerRegistryUrl = dockerRegistryUrl;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends ArtifactServerYaml {
    @Builder
    public Yaml(String type, String harnessApiVersion, String url, String username, String password) {
      super(type, harnessApiVersion, url, username, password);
    }
  }
}
