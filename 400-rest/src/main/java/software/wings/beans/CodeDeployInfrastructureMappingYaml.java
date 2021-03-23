package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CodeDeployInfrastructureMappingYaml extends InfrastructureMappingYamlWithComputeProvider {
  private String region;
  private String applicationName;
  private String deploymentGroup;
  private String deploymentConfig;
  private String hostNameConvention;

  @Builder
  public CodeDeployInfrastructureMappingYaml(String type, String harnessApiVersion, String computeProviderType,
      String serviceName, String infraMappingType, String deploymentType, String computeProviderName, String region,
      String applicationName, String deploymentGroup, String deploymentConfig, String hostNameConvention,
      Map<String, Object> blueprints) {
    super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
        computeProviderName, blueprints);
    this.region = region;
    this.applicationName = applicationName;
    this.deploymentGroup = deploymentGroup;
    this.deploymentConfig = deploymentConfig;
    this.hostNameConvention = hostNameConvention;
  }
}
