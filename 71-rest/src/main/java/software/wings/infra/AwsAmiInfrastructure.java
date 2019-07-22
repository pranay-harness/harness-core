package software.wings.infra;

import static software.wings.beans.AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

import java.util.List;

@JsonTypeName("AWS_AMI")
@Data
@Builder
public class AwsAmiInfrastructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  @ExcludeFieldMap private String cloudProviderId;

  private String region;

  private String autoScalingGroupName;

  private List<String> classicLoadBalancers;

  private List<String> targetGroupArns;

  private String hostNameConvention;

  // Variables for B/G type Ami deployment
  private List<String> stageClassicLoadBalancers;

  private List<String> stageTargetGroupArns;

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

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AWS;
  }
}
