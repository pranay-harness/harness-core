package software.wings.infra;

import static software.wings.beans.AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.annotation.IncludeInFieldMap;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsAmiInfrastructureMapping.AwsAmiInfrastructureMappingKeys;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

import java.util.List;

@JsonTypeName("AWS_AMI")
@Data
public class AwsAmiInfrastructure implements InfraMappingInfrastructureProvider {
  private String cloudProviderId;

  @IncludeInFieldMap(key = AwsAmiInfrastructureMappingKeys.region) private String region;

  @IncludeInFieldMap(key = AwsAmiInfrastructureMappingKeys.autoScalingGroupName) private String autoScalingGroupName;

  @IncludeInFieldMap(key = AwsAmiInfrastructureMappingKeys.classicLoadBalancers)
  private List<String> classicLoadBalancers;

  @IncludeInFieldMap(key = AwsAmiInfrastructureMappingKeys.targetGroupArns) private List<String> targetGroupArns;

  @IncludeInFieldMap(key = AwsAmiInfrastructureMappingKeys.hostNameConvention) private String hostNameConvention;

  // Variables for B/G type Ami deployment
  @IncludeInFieldMap(key = AwsAmiInfrastructureMappingKeys.stageClassicLoadBalancers)
  private List<String> stageClassicLoadBalancers;

  @IncludeInFieldMap(key = AwsAmiInfrastructureMappingKeys.stageClassicLoadBalancers)
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
}
