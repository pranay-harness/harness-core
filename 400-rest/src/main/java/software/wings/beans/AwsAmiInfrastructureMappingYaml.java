package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AwsAmiInfrastructureMappingYaml extends InfrastructureMappingYamlWithComputeProvider {
  private String region;
  private String autoScalingGroupName;
  private List<String> classicLoadBalancers;
  private List<String> targetGroupArns;
  private String hostNameConvention;
  private List<String> stageClassicLoadBalancers;
  private List<String> stageTargetGroupArns;
  private AmiDeploymentType amiDeploymentType;
  private String spotinstElastiGroupJson;
  private String spotinstCloudProviderName;

  @lombok.Builder
  public AwsAmiInfrastructureMappingYaml(String type, String harnessApiVersion, String computeProviderType,
      String serviceName, String infraMappingType, String deploymentType, String computeProviderName, String region,
      String autoScalingGroupName, List<String> classicLoadBalancers, List<String> targetGroupArns,
      String hostNameConvention, List<String> stageClassicLoadBalancers, List<String> stageTargetGroupArns,
      Map<String, Object> blueprints, AmiDeploymentType amiDeploymentType, String spotinstElastiGroupJson,
      String spotinstCloudProviderName) {
    super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
        computeProviderName, blueprints);
    this.region = region;
    this.autoScalingGroupName = autoScalingGroupName;
    this.classicLoadBalancers = classicLoadBalancers;
    this.targetGroupArns = targetGroupArns;
    this.hostNameConvention = hostNameConvention;
    this.stageClassicLoadBalancers = stageClassicLoadBalancers;
    this.stageTargetGroupArns = stageTargetGroupArns;
    this.amiDeploymentType = amiDeploymentType;
    this.spotinstElastiGroupJson = spotinstElastiGroupJson;
    this.spotinstCloudProviderName = spotinstCloudProviderName;
  }
}
