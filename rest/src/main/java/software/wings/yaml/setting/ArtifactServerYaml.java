package software.wings.yaml.setting;

import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.settings.SettingValue;

/**
 * @author rktummala on 11/18/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public abstract class ArtifactServerYaml extends SettingValue.Yaml {
  private String url;
  private String username;
  private String password = ENCRYPTED_VALUE_STR;

  public ArtifactServerYaml(String type, String harnessApiVersion, String url, String username, String password) {
    super(type, harnessApiVersion);
    this.url = url;
    this.username = username;
    this.password = password;
  }
}
