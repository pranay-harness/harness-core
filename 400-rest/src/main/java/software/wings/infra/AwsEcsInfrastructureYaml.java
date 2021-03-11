package software.wings.infra;

import static software.wings.beans.InfrastructureType.AWS_ECS;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(AWS_ECS)
public final class AwsEcsInfrastructureYaml extends CloudProviderInfrastructureYaml {
  private String cloudProviderName;
  private String region;
  private String vpcId;
  private List<String> subnetIds;
  private List<String> securityGroupIds;
  private boolean assignPublicIp;
  private String executionRole;
  private String launchType;
  private Map<String, String> expressions;
  private String clusterName;

  @Builder
  public AwsEcsInfrastructureYaml(String type, String cloudProviderName, String region, String vpcId,
      List<String> subnetIds, List<String> securityGroupIds, boolean assignPublicIp, String executionRole,
      String launchType, Map<String, String> expressions, String clusterName) {
    super(type);
    setCloudProviderName(cloudProviderName);
    setRegion(region);
    setVpcId(vpcId);
    setSubnetIds(subnetIds);
    setSecurityGroupIds(securityGroupIds);
    setAssignPublicIp(assignPublicIp);
    setExecutionRole(executionRole);
    setLaunchType(launchType);
    setExpressions(expressions);
    setClusterName(clusterName);
  }

  public AwsEcsInfrastructureYaml() {
    super(AWS_ECS);
  }
}
