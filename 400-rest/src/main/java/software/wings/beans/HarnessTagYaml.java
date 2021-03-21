package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.yaml.BaseEntityYaml;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HarnessTagYaml extends BaseEntityYaml {
  private List<HarnessTagAbstractYaml> tag;

  @Builder
  public HarnessTagYaml(String harnessApiVersion, List<HarnessTagAbstractYaml> tag) {
    super(EntityType.TAG.name(), harnessApiVersion);
    this.tag = tag;
  }
}
