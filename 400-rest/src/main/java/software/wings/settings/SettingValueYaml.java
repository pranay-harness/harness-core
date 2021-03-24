package software.wings.settings;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.yaml.BaseEntityYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class SettingValueYaml extends BaseEntityYaml {
  private UsageRestrictionsYaml usageRestrictions;

  public SettingValueYaml(String type, String harnessApiVersion, UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion);
    this.usageRestrictions = usageRestrictions;
  }
}
