package software.wings.yaml.setting;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.settings.SettingValue;

/**
 * @author rktummala on 11/18/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class LoadBalancerProviderYaml extends SettingValue.Yaml {
  public LoadBalancerProviderYaml() {}

  public LoadBalancerProviderYaml(String type, String name) {
    super(type, name);
  }
}
