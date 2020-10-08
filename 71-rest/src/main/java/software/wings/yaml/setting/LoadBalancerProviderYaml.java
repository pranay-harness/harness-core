package software.wings.yaml.setting;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.security.UsageRestrictions;
import software.wings.settings.SettingValue;

/**
 * @author rktummala on 11/18/17
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class LoadBalancerProviderYaml extends SettingValue.Yaml {
  public LoadBalancerProviderYaml(String type, String harnessApiVersion, UsageRestrictions.Yaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
  }
}
