package software.wings.yaml.setting;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class VerificationProviderYaml extends SettingValue.Yaml {
  public VerificationProviderYaml(String type, String harnessApiVersion, UsageRestrictions.Yaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
  }
}