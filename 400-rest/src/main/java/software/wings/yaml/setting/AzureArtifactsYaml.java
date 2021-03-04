package software.wings.yaml.setting;

import software.wings.security.UsageRestrictionYaml;
import software.wings.settings.SettingValue;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AzureArtifactsYaml extends SettingValue.Yaml {
  public AzureArtifactsYaml(String type, String harnessApiVersion, UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
  }
}
