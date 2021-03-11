package software.wings.infra;

import static software.wings.beans.InfrastructureType.AWS_AMI;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.AmiDeploymentType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(AWS_AMI)
public final class AwsAmiInfrastructureYaml extends CloudProviderInfrastructureYaml {
  private String cloudProviderName;
  private String region;
  private String autoScalingGroupName;
  private List<String> classicLoadBalancers;
  private List<String> targetGroupArns;
  private String hostNameConvention;

  // Variables for B/G type Ami deployment
  private List<String> stageClassicLoadBalancers;
  private List<String> stageTargetGroupArns;
  private Map<String, String> expressions;

  private AmiDeploymentType amiDeploymentType;
  private String spotinstElastiGroupJson;
  private String spotinstCloudProviderName;
  boolean asgIdentifiesWorkload;
  boolean useTrafficShift;

  @Builder
  public AwsAmiInfrastructureYaml(String type, String cloudProviderName, String region, String autoScalingGroupName,
      List<String> classicLoadBalancers, List<String> targetGroupArns, String hostNameConvention,
      List<String> stageClassicLoadBalancers, List<String> stageTargetGroupArns, Map<String, String> expressions,
      AmiDeploymentType amiDeploymentType, String spotinstElastiGroupJson, String spotinstCloudProviderName,
      boolean asgIdentifiesWorkload, boolean useTrafficShift) {
    super(type);
    setCloudProviderName(cloudProviderName);
    setRegion(region);
    setAutoScalingGroupName(autoScalingGroupName);
    setClassicLoadBalancers(classicLoadBalancers);
    setTargetGroupArns(targetGroupArns);
    setHostNameConvention(hostNameConvention);
    setStageClassicLoadBalancers(stageClassicLoadBalancers);
    setStageTargetGroupArns(stageTargetGroupArns);
    setExpressions(expressions);
    setAmiDeploymentType(amiDeploymentType);
    setSpotinstCloudProviderName(spotinstCloudProviderName);
    setSpotinstElastiGroupJson(spotinstElastiGroupJson);
    setAsgIdentifiesWorkload(asgIdentifiesWorkload);
    setUseTrafficShift(useTrafficShift);
  }

  public AwsAmiInfrastructureYaml() {
    super(AWS_AMI);
  }
}
