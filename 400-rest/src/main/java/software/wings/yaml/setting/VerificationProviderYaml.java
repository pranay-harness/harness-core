package software.wings.yaml.setting;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.settings.SettingValueYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class VerificationProviderYaml extends SettingValueYaml {
  public VerificationProviderYaml(String type, String harnessApiVersion, UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
  }
}
