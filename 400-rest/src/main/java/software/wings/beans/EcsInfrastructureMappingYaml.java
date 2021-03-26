package software.wings.beans;

import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.amazonaws.services.ecs.model.LaunchType;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class EcsInfrastructureMappingYaml extends ContainerInfrastructureMappingYamlWithComputeProvider {
  private String region = AWS_DEFAULT_REGION;
  private String vpcId;
  private String subnetIds;
  private String securityGroupIds;
  private String launchType = LaunchType.EC2.name();
  private boolean assignPublicIp;
  private String executionRole;

  @lombok.Builder
  public EcsInfrastructureMappingYaml(String type, String harnessApiVersion, String computeProviderType,
      String serviceName, String infraMappingType, String deploymentType, String computeProviderName, String cluster,
      String region, String vpcId, String subnetIds, String securityGroupIds, String launchType, boolean assignPublicIp,
      String executionRole, Map<String, Object> blueprints) {
    super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
        computeProviderName, cluster, blueprints);
    this.region = region;
    this.vpcId = vpcId;
    this.subnetIds = subnetIds;
    this.securityGroupIds = securityGroupIds;
    this.launchType = launchType;
    this.assignPublicIp = assignPublicIp;
    this.executionRole = executionRole;
  }
}
