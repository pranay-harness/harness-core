package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.yaml.BaseEntityYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public abstract class DeploymentSpecificationYaml extends BaseEntityYaml {
  public DeploymentSpecificationYaml(String type, String harnessApiVersion) {
    super(type, harnessApiVersion);
  }
}
