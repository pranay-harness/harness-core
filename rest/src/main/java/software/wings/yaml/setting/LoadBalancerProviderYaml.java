package software.wings.yaml.setting;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;

/**
 * @author rktummala on 11/18/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public abstract class LoadBalancerProviderYaml extends SettingValue.Yaml {
  public LoadBalancerProviderYaml(String type, String harnessApiVersion, UsageRestrictions usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
  }
}
