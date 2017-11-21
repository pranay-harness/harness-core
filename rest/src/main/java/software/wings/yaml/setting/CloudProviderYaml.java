package software.wings.yaml.setting;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.settings.SettingValue;

/**
 * @author rktummala on 11/18/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class CloudProviderYaml extends SettingValue.Yaml {
  public CloudProviderYaml(String type, String name) {
    super(type, name);
  }
}
