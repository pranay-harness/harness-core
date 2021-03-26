package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HarnessTagAbstractYaml extends BaseYaml {
  private String name;
  private List<String> allowedValues;

  @lombok.Builder
  public HarnessTagAbstractYaml(String name, List<String> allowedValues) {
    this.name = name;
    this.allowedValues = allowedValues;
  }
}
