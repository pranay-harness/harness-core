package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AwsLambdaInfraStructureMappingYaml extends InfrastructureMapping.YamlWithComputeProvider {
  private String region;
  private String vpcId;
  private List<String> subnetIds = new ArrayList<>();
  private List<String> securityGroupIds = new ArrayList<>();
  private String role;

  @lombok.Builder
  public AwsLambdaInfraStructureMappingYaml(String type, String harnessApiVersion, String computeProviderType,
      String serviceName, String infraMappingType, String deploymentType, String computeProviderName, String region,
      String vpcId, List<String> subnetIds, List<String> securityGroupIds, String role,
      Map<String, Object> blueprints) {
    super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
        computeProviderName, blueprints);
    this.region = region;
    this.vpcId = vpcId;
    this.subnetIds = subnetIds;
    this.securityGroupIds = securityGroupIds;
    this.role = role;
  }
}
