package software.wings.yaml.setting;

import software.wings.security.UsageRestrictionYaml;
import software.wings.settings.SettingValue;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HelmRepoYaml extends SettingValue.Yaml {
  public HelmRepoYaml(String type, String harnessApiVersion, UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
  }
}
