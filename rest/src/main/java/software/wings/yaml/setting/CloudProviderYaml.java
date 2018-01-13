package software.wings.yaml.setting;

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
public abstract class CloudProviderYaml extends SettingValue.Yaml {
  public CloudProviderYaml(String type, String harnessApiVersion) {
    super(type, harnessApiVersion);
  }
}
