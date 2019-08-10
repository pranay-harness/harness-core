package software.wings.infra;

import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.InfrastructureType.AWS_INSTANCE;

import com.google.common.collect.ImmutableSet;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.AwsInstanceFilter.AwsInstanceFilterKeys;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import java.util.Map;
import java.util.Set;

@JsonTypeName("AWS_SSH")
@Data
@Builder
@FieldNameConstants(innerTypeName = "AwsInstanceInfrastructureKeys")
public class AwsInstanceInfrastructure
    implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider, SshBasedInfrastructure, ProvisionerAware {
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
  @ExcludeFieldMap private Map<String, String> expressions;

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

  public String getInfrastructureType() {
    return AWS_INSTANCE;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AWS;
  }

  @Override
  public Set<String> getSupportedExpressions() {
    return ImmutableSet.of(AwsInstanceInfrastructureKeys.autoScalingGroupName, AwsInstanceInfrastructureKeys.region,
        AwsInstanceFilterKeys.vpcIds, AwsInstanceFilterKeys.tags, AwsInstanceInfrastructureKeys.loadBalancerId);
  }

  @Override
  public void applyExpressions(Map<String, Object> resolvedExpressions) {}

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(AWS_INSTANCE)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
    private String cloudProviderName;
    private String region;
    private String hostConnectionAttrsName;
    private String loadBalancerName;
    private boolean usePublicDns;
    private boolean useAutoScalingGroup;
    private AwsInstanceFilter awsInstanceFilter;
    private String autoScalingGroupName;
    private boolean setDesiredCapacity;
    private int desiredCapacity;
    private String hostNameConvention;
    private Map<String, String> expressions;

    @Builder
    public Yaml(String type, String cloudProviderName, String region, String hostConnectionAttrsName,
        String loadBalancerName, boolean usePublicDns, boolean useAutoScalingGroup, AwsInstanceFilter awsInstanceFilter,
        String autoScalingGroupName, boolean setDesiredCapacity, int desiredCapacity, String hostNameConvention,
        Map<String, String> expressions) {
      super(type);
      setCloudProviderName(cloudProviderName);
      setRegion(region);
      setHostConnectionAttrsName(hostConnectionAttrsName);
      setLoadBalancerName(loadBalancerName);
      setUsePublicDns(usePublicDns);
      setUseAutoScalingGroup(useAutoScalingGroup);
      setAwsInstanceFilter(awsInstanceFilter);
      setDesiredCapacity(desiredCapacity);
      setHostNameConvention(hostNameConvention);
      setExpressions(expressions);
    }

    public Yaml() {
      super(AWS_INSTANCE);
    }
  }
}
