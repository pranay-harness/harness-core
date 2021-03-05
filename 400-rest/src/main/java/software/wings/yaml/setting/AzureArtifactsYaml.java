package software.wings.yaml.setting;

import software.wings.security.UsageRestrictionYaml;
import software.wings.settings.SettingValueYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AzureArtifactsYaml extends SettingValueYaml {
  public AzureArtifactsYaml(String type, String harnessApiVersion, UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
  }
}
