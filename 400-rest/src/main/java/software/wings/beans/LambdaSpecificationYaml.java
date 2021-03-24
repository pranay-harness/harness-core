package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class LambdaSpecificationYaml extends DeploymentSpecificationYaml {
  private LambdaSpecificationDefaultSpecificationYaml defaults;
  private List<LambdaSpecification.FunctionSpecification.Yaml> functions;

  @Builder
  public LambdaSpecificationYaml(String type, String harnessApiVersion,
      LambdaSpecificationDefaultSpecificationYaml defaults,
      List<LambdaSpecification.FunctionSpecification.Yaml> functions) {
    super(type, harnessApiVersion);
    this.defaults = defaults;
    this.functions = functions;
  }
}
