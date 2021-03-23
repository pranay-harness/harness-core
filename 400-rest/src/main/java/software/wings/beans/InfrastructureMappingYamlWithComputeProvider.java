package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class InfrastructureMappingYamlWithComputeProvider extends InfrastructureMappingYaml {
  private String computeProviderType;
  private String computeProviderName;

  public InfrastructureMappingYamlWithComputeProvider(String type, String harnessApiVersion, String serviceName,
      String infraMappingType, String deploymentType, String computeProviderType, String computeProviderName,
      Map<String, Object> blueprints) {
    super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, blueprints);
    this.computeProviderType = computeProviderType;
    this.computeProviderName = computeProviderName;
  }
}
