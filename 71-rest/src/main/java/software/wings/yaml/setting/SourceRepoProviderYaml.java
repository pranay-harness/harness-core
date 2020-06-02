package software.wings.yaml.setting;

import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;

/**
 * @author dhruvupadhyay on 01/06/20
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class SourceRepoProviderYaml extends SettingValue.Yaml {
  private String url;
  private String username;
  private String password = ENCRYPTED_VALUE_STR;

  public SourceRepoProviderYaml(String type, String harnessApiVersion, String url, String username, String password,
      UsageRestrictions.Yaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.url = url;
    this.username = username;
    this.password = password;
  }
}