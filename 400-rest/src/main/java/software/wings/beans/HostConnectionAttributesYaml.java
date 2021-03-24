package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.settings.SettingValue;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class HostConnectionAttributesYaml extends SettingValue.Yaml {
  private String connectionType;
  private String accessType;
  private String userName;
  private String key;

  @lombok.Builder
  public HostConnectionAttributesYaml(String type, String harnessApiVersion, String connectionType, String accessType,
      String userName, String key, UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.connectionType = connectionType;
    this.accessType = accessType;
    this.userName = userName;
    this.key = key;
  }
}
