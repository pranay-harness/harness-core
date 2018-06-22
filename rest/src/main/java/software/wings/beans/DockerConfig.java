package software.wings.beans;

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

/**
 * Created by anubhaw on 1/5/17.
 */
@JsonTypeName("DOCKER")
@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "password")
public class DockerConfig extends SettingValue implements Encryptable, ArtifactSourceable {
  @Attributes(title = "Docker Registry URL", required = true) @NotEmpty private String dockerRegistryUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @Attributes(title = "Password", required = true) @Encrypted private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new Docker registry config.
   */
  public DockerConfig() {
    super(SettingVariableTypes.DOCKER.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public DockerConfig(
      String dockerRegistryUrl, String username, char[] password, String accountId, String encryptedPassword) {
    super(SettingVariableTypes.DOCKER.name());
    setDockerRegistryUrl(dockerRegistryUrl);
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  // override the setter for URL to enforce that we always put / (slash) at the end
  public void setDockerRegistryUrl(String dockerRegistryUrl) {
    this.dockerRegistryUrl = dockerRegistryUrl.endsWith("/") ? dockerRegistryUrl : dockerRegistryUrl.concat("/");
  }

  @Override
  public String fetchUserName() {
    return username;
  }

  @Override
  public String fetchRegistryUrl() {
    return dockerRegistryUrl;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends ArtifactServerYaml {
    @Builder
    public Yaml(String type, String harnessApiVersion, String url, String username, String password,
        UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, url, username, password, usageRestrictions);
    }
  }
}
