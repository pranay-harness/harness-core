package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class LambdaSpecificationDefaultSpecificationYaml extends BaseYaml {
  private String runtime;
  private Integer memorySize = 128;
  private Integer timeout = 3;

  @Builder
  public LambdaSpecificationDefaultSpecificationYaml(String runtime, Integer memorySize, Integer timeout) {
    this.runtime = runtime;
    this.memorySize = memorySize;
    this.timeout = timeout;
  }
}
