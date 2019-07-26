package software.wings.infra;

import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.InfrastructureType.AWS_INSTANCE;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

@JsonTypeName("AWS_SSH")
@Data
@Builder
public class AwsInstanceInfrastructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  @ExcludeFieldMap private String cloudProviderId;

  @ExcludeFieldMap private boolean useAutoScalingGroup;

  private String region;

  private String hostConnectionAttrs;

  private String loadBalancerId;

  @ExcludeFieldMap @Transient private String loadBalancerName;

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

  public String getCloudProviderInfrastructureType() {
    return AWS_INSTANCE;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AWS;
  }
  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(AWS_INSTANCE)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
    private String cloudProviderName;
    private String region;
    private String hostConnectionAttrs;
    private String loadBalancerName;
    private boolean usePublicDns;
    private boolean useAutoScalingGroup;
    private AwsInstanceFilter awsInstanceFilter;
    private String autoScalingGroupName;
    private boolean setDesiredCapacity;
    private int desiredCapacity;
    private String hostNameConvention;

    @Builder
    public Yaml(String type, String cloudProviderName, String region, String hostConnectionAttrs,
        String loadBalancerName, boolean usePublicDns, boolean useAutoScalingGroup, AwsInstanceFilter awsInstanceFilter,
        String autoScalingGroupName, boolean setDesiredCapacity, int desiredCapacity, String hostNameConvention) {
      super(type);
      setCloudProviderName(cloudProviderName);
      setRegion(region);
      setHostConnectionAttrs(hostConnectionAttrs);
      setLoadBalancerName(loadBalancerName);
      setUsePublicDns(usePublicDns);
      setUseAutoScalingGroup(useAutoScalingGroup);
      setAwsInstanceFilter(awsInstanceFilter);
      setDesiredCapacity(desiredCapacity);
      setHostNameConvention(hostNameConvention);
    }

    public Yaml() {
      super(AWS_INSTANCE);
    }
  }
}
