package software.wings.yaml.setting;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.security.UsageRestrictions;
import software.wings.settings.SettingValue;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AzureArtifactsYaml extends SettingValue.Yaml {
  public AzureArtifactsYaml(String type, String harnessApiVersion, UsageRestrictions.Yaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
  }
}
