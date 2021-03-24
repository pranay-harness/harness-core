package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class LambdaSpecificationFunctionSpecificationYaml extends BaseYaml {
  private String runtime;
  private Integer memorySize = 128;
  private Integer timeout = 3;
  private String functionName;
  private String handler;

  @Builder
  public LambdaSpecificationFunctionSpecificationYaml(
      String runtime, Integer memorySize, Integer timeout, String functionName, String handler) {
    this.runtime = runtime;
    this.memorySize = memorySize;
    this.timeout = timeout;
    this.functionName = functionName;
    this.handler = handler;
  }
}
