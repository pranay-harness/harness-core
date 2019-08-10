package software.wings.infra;

import static software.wings.beans.AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping;
import static software.wings.beans.InfrastructureType.AWS_AMI;

import com.google.common.collect.ImmutableSet;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonTypeName("AWS_AMI")
@Data
@Builder
@FieldNameConstants(innerTypeName = "AwsAmiInfrastructureKeys")
public class AwsAmiInfrastructure
    implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider, ProvisionerAware {
  @ExcludeFieldMap private String cloudProviderId;
  private String region;
  private String autoScalingGroupName;
  private List<String> classicLoadBalancers;
  private List<String> targetGroupArns;
  private String hostNameConvention;

  // Variables for B/G type Ami deployment
  private List<String> stageClassicLoadBalancers;
  private List<String> stageTargetGroupArns;

  @ExcludeFieldMap private Map<String, String> expressions;

  @Override
  public InfrastructureMapping getInfraMapping() {
    return anAwsAmiInfrastructureMapping()
        .withComputeProviderSettingId(cloudProviderId)
        .withInfraMappingType(InfrastructureMappingType.AWS_AMI.name())
        .withRegion(region)
        .withAutoScalingGroupName(autoScalingGroupName)
        .withClassicLoadBalancers(classicLoadBalancers)
        .withTargetGroupArns(targetGroupArns)
        .withHostNameConvention(hostNameConvention)
        .withStageClassicLoadBalancers(stageClassicLoadBalancers)
        .withStageTargetGroupArns(stageTargetGroupArns)
        .withInfraMappingType(InfrastructureMappingType.AWS_AMI.name())
        .build();
  }

  @Override
  public Class<AwsAmiInfrastructureMapping> getMappingClass() {
    return AwsAmiInfrastructureMapping.class;
  }

  public String getInfrastructureType() {
    return AWS_AMI;
  }

  @Override
  public Set<String> getSupportedExpressions() {
    return ImmutableSet.of(AwsAmiInfrastructureKeys.region, AwsAmiInfrastructureKeys.autoScalingGroupName,
        AwsAmiInfrastructureKeys.classicLoadBalancers, AwsAmiInfrastructureKeys.targetGroupArns,
        AwsAmiInfrastructureKeys.stageClassicLoadBalancers, AwsAmiInfrastructureKeys.stageTargetGroupArns);
  }

  @Override
  public void applyExpressions(Map<String, Object> resolvedExpressions) {}

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(AWS_AMI)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
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

    @Builder
    public Yaml(String type, String cloudProviderName, String region, String autoScalingGroupName,
        List<String> classicLoadBalancers, List<String> targetGroupArns, String hostNameConvention,
        List<String> stageClassicLoadBalancers, List<String> stageTargetGroupArns, Map<String, String> expressions) {
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
    }

    public Yaml() {
      super(AWS_AMI);
    }
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AWS;
  }
}
