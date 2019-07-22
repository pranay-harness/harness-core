package software.wings.infra;

import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

@JsonTypeName("AWS_SSH")
@Data
public class AwsInstanceInfrastructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  @ExcludeFieldMap private String cloudProviderId;

  private boolean useAutoScalingGroup;

  private String region;

  private String hostConnectionAttrs;

  private String loadBalancerId;

  @Transient private String loadBalancerName;

  private boolean usePublicDns;

  private AwsInstanceFilter awsInstanceFilter;

  private String autoScalingGroupName;

  private boolean setDesiredCapacity;

  private int desiredCapacity;

  private String hostNameConvention;

  private boolean provisionInstances;

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

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AWS;
  }
}
