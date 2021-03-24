package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import software.wings.settings.SettingValueYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SettingAttributeYaml extends BaseYaml {
  private String name;
  private SettingValueYaml value;

  @lombok.Builder
  public SettingAttributeYaml(String name, SettingValueYaml value) {
    this.name = name;
    this.value = value;
  }
}
