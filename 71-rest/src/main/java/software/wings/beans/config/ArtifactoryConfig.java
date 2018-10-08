package software.wings.beans.config;

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
import software.wings.annotation.EncryptableSetting;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.ArtifactServerYaml;

/**
 * Created by sgurubelli on 6/20/17.
 */
@JsonTypeName("ARTIFACTORY")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "password")
@Builder
public class ArtifactoryConfig extends SettingValue implements EncryptableSetting, ArtifactSourceable {
  @Attributes(title = "Artifactory URL", required = true) @NotEmpty private String artifactoryUrl;

  @Attributes(title = "Username") private String username;

  @Attributes(title = "Password") @Encrypted private char[] password;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  public ArtifactoryConfig() {
    super(SettingVariableTypes.ARTIFACTORY.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ArtifactoryConfig(
      String artifactoryUrl, String username, char[] password, String accountId, String encryptedPassword) {
    this();
    this.artifactoryUrl = artifactoryUrl;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Override
  public String fetchUserName() {
    return username;
  }

  @Override
  public String fetchRegistryUrl() {
    return artifactoryUrl;
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
