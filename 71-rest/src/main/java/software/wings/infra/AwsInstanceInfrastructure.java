package software.wings.infra;

import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.IncludeInFieldMap;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInfrastructureMapping.AwsInfrastructureMappingKeys;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

@JsonTypeName("AWS_SSH")
@Data
public class AwsInstanceInfrastructure implements InfraMappingInfrastructureProvider {
  private String cloudProviderId;

  private boolean useAutoScalingGroup;

  @IncludeInFieldMap(key = AwsInfrastructureMappingKeys.region) private String region;

  @IncludeInFieldMap(key = AwsInfrastructureMappingKeys.hostConnectionAttrs) private String hostConnectionAttrs;

  @IncludeInFieldMap(key = AwsInfrastructureMappingKeys.loadBalancerId) private String loadBalancerId;

  @Transient private String loadBalancerName;

  @IncludeInFieldMap(key = AwsInfrastructureMappingKeys.usePublicDns) private boolean usePublicDns;

  @IncludeInFieldMap(key = AwsInfrastructureMappingKeys.awsInstanceFilter) private AwsInstanceFilter awsInstanceFilter;

  @IncludeInFieldMap(key = AwsInfrastructureMappingKeys.autoScalingGroupName) private String autoScalingGroupName;

  @IncludeInFieldMap(key = AwsInfrastructureMappingKeys.setDesiredCapacity) private boolean setDesiredCapacity;

  @IncludeInFieldMap(key = AwsInfrastructureMappingKeys.desiredCapacity) private int desiredCapacity;

  @IncludeInFieldMap(key = AwsInfrastructureMappingKeys.hostNameConvention) private String hostNameConvention;

  @IncludeInFieldMap(key = AwsInfrastructureMappingKeys.provisionInstances) private boolean provisionInstances;

  @Override
  public InfrastructureMapping getInfraMapping() {
    return anAwsInfrastructureMapping()
        .withComputeProviderSettingId(cloudProviderId)
        .withRegion(region)
        .withHostConnectionAttrs(hostConnectionAttrs)
        .withLoadBalancerId(loadBalancerId)
        .withUsePublicDns(usePublicDns)
        .withAwsInstanceFilter(awsInstanceFilter)
        .withAutoScalingGroupName(autoScalingGroupName)
        .withSetDesiredCapacity(setDesiredCapacity)
        .withDesiredCapacity(desiredCapacity)
        .withHostNameConvention(hostNameConvention)
        .withProvisionInstances(provisionInstances)
        .withInfraMappingType(InfrastructureMappingType.AWS_SSH.name())
        .build();
  }

  @Override
  public Class<AwsInfrastructureMapping> getMappingClass() {
    return AwsInfrastructureMapping.class;
  }
}
