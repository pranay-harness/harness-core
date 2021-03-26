package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class ContainerInfrastructureMappingYamlWithComputeProvider
    extends InfrastructureMappingYamlWithComputeProvider {
  private String cluster;

  public ContainerInfrastructureMappingYamlWithComputeProvider(String type, String harnessApiVersion,
      String serviceName, String infraMappingType, String deploymentType, String computeProviderType,
      String computeProviderName, String cluster, Map<String, Object> blueprints) {
    super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, computeProviderType,
        computeProviderName, blueprints);
    this.cluster = cluster;
  }
}
