package software.wings.beans;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.StringUtils.isEmpty;

import com.google.common.collect.ImmutableMap;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.app.MainConfiguration;
import software.wings.exception.WingsException;
import software.wings.stencils.DataProvider;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeName("AWS_SSH")
public class AwsInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Restrictions") @SchemaIgnore private String restrictionType;
  @Attributes(title = "Expression") @SchemaIgnore private String restrictionExpression;

  @Attributes(title = "Region")
  @DefaultValue("us-east-1")
  @EnumData(enumDataProvider = AwsRegionDataProvider.class)
  private String region;

  @EnumData(enumDataProvider = HostConnectionAttributesDataProvider.class)
  @Attributes(title = "Connection Type", required = true)
  @NotEmpty
  private String hostConnectionAttrs;
  @Attributes(title = "Load Balancer") private String loadBalancerId;
  @Transient @SchemaIgnore private String loadBalancerName;

  @Attributes(title = "Use Public DNS for SSH connection") private boolean usePublicDns;

  @Attributes(title = "Provision Instances") private boolean provisionInstances;

  @Attributes(title = "Instance Filter") private AwsInstanceFilter awsInstanceFilter;

  @Attributes(title = "AutoScaling group") private String autoScalingGroupName;

  /**
   * Instantiates a new Aws infrastructure mapping.
   */
  public AwsInfrastructureMapping() {
    super(InfrastructureMappingType.AWS_SSH.name());
  }

  public void validate() {
    if (provisionInstances) {
      if (isEmpty(autoScalingGroupName)) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args",
            "Auto scaling group must not be empty when provision instances is true.");
      }
    } else {
      if (awsInstanceFilter == null) {
        throw new WingsException(
            ErrorCode.INVALID_ARGUMENT, "args", "Instance filter must not be null when provision instances is false.");
      }
    }
  }

  /**
   * Gets restriction type.
   *
   * @return the restriction type
   */
  public String getRestrictionType() {
    return restrictionType;
  }

  /**
   * Sets restriction type.
   *
   * @param restrictionType the restriction type
   */
  public void setRestrictionType(String restrictionType) {
    this.restrictionType = restrictionType;
  }

  /**
   * Gets restriction expression.
   *
   * @return the restriction expression
   */
  public String getRestrictionExpression() {
    return restrictionExpression;
  }

  /**
   * Sets restriction expression.
   *
   * @param restrictionExpression the restriction expression
   */
  public void setRestrictionExpression(String restrictionExpression) {
    this.restrictionExpression = restrictionExpression;
  }

  /**
   * Gets load balancer id.
   *
   * @return the load balancer id
   */
  public String getLoadBalancerId() {
    return loadBalancerId;
  }

  /**
   * Sets load balancer id.
   *
   * @param loadBalancerId the load balancer id
   */
  public void setLoadBalancerId(String loadBalancerId) {
    this.loadBalancerId = loadBalancerId;
  }

  @Override
  public String getHostConnectionAttrs() {
    return hostConnectionAttrs;
  }

  @SchemaIgnore
  @Override
  public String getDisplayName() {
    return String.format("%s (AWS/SSH) %s",
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
        this.getRegion());
  }

  /**
   * Gets region.
   *
   * @return the region
   */
  public String getRegion() {
    return region;
  }

  /**
   * Sets region.
   *
   * @param region the region
   */
  public void setRegion(String region) {
    this.region = region;
  }

  /**
   * Sets host connection attrs.
   *
   * @param hostConnectionAttrs the host connection attrs
   */
  public void setHostConnectionAttrs(String hostConnectionAttrs) {
    this.hostConnectionAttrs = hostConnectionAttrs;
  }

  /**
   * Gets load balancer name.
   *
   * @return the load balancer name
   */
  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  /**
   * Sets load balancer name.
   *
   * @param loadBalancerName the load balancer name
   */
  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  /**
   * Gets aws instance filter.
   *
   * @return the aws instance filter
   */
  public AwsInstanceFilter getAwsInstanceFilter() {
    return awsInstanceFilter;
  }

  /**
   * Sets aws instance filter.
   *
   * @param awsInstanceFilter the aws instance filter
   */
  public void setAwsInstanceFilter(AwsInstanceFilter awsInstanceFilter) {
    this.awsInstanceFilter = awsInstanceFilter;
  }

  public boolean isProvisionInstances() {
    return provisionInstances;
  }

  public void setProvisionInstances(boolean provisionInstances) {
    this.provisionInstances = provisionInstances;
  }

  public boolean isUsePublicDns() {
    return usePublicDns;
  }

  public void setUsePublicDns(boolean usePublicDns) {
    this.usePublicDns = usePublicDns;
  }

  public String getAutoScalingGroupName() {
    return autoScalingGroupName;
  }

  public void setAutoScalingGroupName(String autoScalingGroupName) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  /**
   * The enum Restriction type.
   */
  public enum RestrictionType {
    /**
     * None restriction type.
     */
    NONE("None"), /**
                   * Instance restriction type.
                   */
    INSTANCE("By specific instances"), /**
                                        * Custom restriction type.
                                        */
    CUSTOM("By zone/tags etc");

    private String displayName;

    RestrictionType(String displayName) {
      this.displayName = displayName;
    }

    /**
     * Gets display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
      return displayName;
    }

    /**
     * Sets display name.
     *
     * @param displayName the display name
     */
    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }
  }

  /**
   * The type Aws infrastructure restriction provider.
   */
  public static class AwsInfrastructureRestrictionProvider implements DataProvider {
    @Override
    public Map<String, String> getData(String appId, String... params) {
      return Arrays.stream(RestrictionType.values())
          .collect(toMap(RestrictionType::name, RestrictionType::getDisplayName));
    }
  }

  /**
   * The type Aws region data provider.
   */
  public static class AwsRegionDataProvider implements DataProvider {
    @Inject private MainConfiguration mainConfiguration;

    @Override
    public Map<String, String> getData(String appId, String... params) {
      return Arrays.stream(Regions.values())
          .filter(regions -> regions != Regions.GovCloud)
          .collect(toMap(Regions::getName,
              regions
              -> Optional.ofNullable(mainConfiguration.getAwsRegionIdToName())
                     .orElse(ImmutableMap.of(regions.getName(), regions.getName()))
                     .get(regions.getName())));
    }
  }

  public static final class Builder {
    protected String appId;
    private String restrictionType;
    private String restrictionExpression;
    private String region;
    private String uuid;
    private String hostConnectionAttrs;
    private EmbeddedUser createdBy;
    private String loadBalancerId;
    private long createdAt;
    private String loadBalancerName;
    private EmbeddedUser lastUpdatedBy;
    private String computeProviderSettingId;
    private long lastUpdatedAt;
    private String envId;
    private boolean usePublicDns;
    private String serviceTemplateId;
    private boolean provisionInstances;
    private String serviceId;
    private String computeProviderType;
    private AwsInstanceFilter awsInstanceFilter;
    private String infraMappingType;
    private String autoScalingGroupName;
    private String deploymentType;
    private String computeProviderName;
    private String displayName;

    private Builder() {}

    public static Builder anAwsInfrastructureMapping() {
      return new Builder();
    }

    public Builder withRestrictionType(String restrictionType) {
      this.restrictionType = restrictionType;
      return this;
    }

    public Builder withRestrictionExpression(String restrictionExpression) {
      this.restrictionExpression = restrictionExpression;
      return this;
    }

    public Builder withRegion(String region) {
      this.region = region;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withHostConnectionAttrs(String hostConnectionAttrs) {
      this.hostConnectionAttrs = hostConnectionAttrs;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withLoadBalancerId(String loadBalancerId) {
      this.loadBalancerId = loadBalancerId;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLoadBalancerName(String loadBalancerName) {
      this.loadBalancerName = loadBalancerName;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withComputeProviderSettingId(String computeProviderSettingId) {
      this.computeProviderSettingId = computeProviderSettingId;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withUsePublicDns(boolean usePublicDns) {
      this.usePublicDns = usePublicDns;
      return this;
    }

    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public Builder withProvisionInstances(boolean provisionInstances) {
      this.provisionInstances = provisionInstances;
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withComputeProviderType(String computeProviderType) {
      this.computeProviderType = computeProviderType;
      return this;
    }

    public Builder withAwsInstanceFilter(AwsInstanceFilter awsInstanceFilter) {
      this.awsInstanceFilter = awsInstanceFilter;
      return this;
    }

    public Builder withInfraMappingType(String infraMappingType) {
      this.infraMappingType = infraMappingType;
      return this;
    }

    public Builder withAutoScalingGroupName(String autoScalingGroupName) {
      this.autoScalingGroupName = autoScalingGroupName;
      return this;
    }

    public Builder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public Builder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder but() {
      return anAwsInfrastructureMapping()
          .withRestrictionType(restrictionType)
          .withRestrictionExpression(restrictionExpression)
          .withRegion(region)
          .withUuid(uuid)
          .withAppId(appId)
          .withHostConnectionAttrs(hostConnectionAttrs)
          .withCreatedBy(createdBy)
          .withLoadBalancerId(loadBalancerId)
          .withCreatedAt(createdAt)
          .withLoadBalancerName(loadBalancerName)
          .withLastUpdatedBy(lastUpdatedBy)
          .withComputeProviderSettingId(computeProviderSettingId)
          .withLastUpdatedAt(lastUpdatedAt)
          .withEnvId(envId)
          .withUsePublicDns(usePublicDns)
          .withServiceTemplateId(serviceTemplateId)
          .withProvisionInstances(provisionInstances)
          .withServiceId(serviceId)
          .withComputeProviderType(computeProviderType)
          .withAwsInstanceFilter(awsInstanceFilter)
          .withInfraMappingType(infraMappingType)
          .withAutoScalingGroupName(autoScalingGroupName)
          .withDeploymentType(deploymentType)
          .withComputeProviderName(computeProviderName)
          .withDisplayName(displayName);
    }

    public AwsInfrastructureMapping build() {
      AwsInfrastructureMapping awsInfrastructureMapping = new AwsInfrastructureMapping();
      awsInfrastructureMapping.setRestrictionType(restrictionType);
      awsInfrastructureMapping.setRestrictionExpression(restrictionExpression);
      awsInfrastructureMapping.setRegion(region);
      awsInfrastructureMapping.setUuid(uuid);
      awsInfrastructureMapping.setAppId(appId);
      awsInfrastructureMapping.setHostConnectionAttrs(hostConnectionAttrs);
      awsInfrastructureMapping.setCreatedBy(createdBy);
      awsInfrastructureMapping.setLoadBalancerId(loadBalancerId);
      awsInfrastructureMapping.setCreatedAt(createdAt);
      awsInfrastructureMapping.setLoadBalancerName(loadBalancerName);
      awsInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      awsInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      awsInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      awsInfrastructureMapping.setEnvId(envId);
      awsInfrastructureMapping.setUsePublicDns(usePublicDns);
      awsInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      awsInfrastructureMapping.setProvisionInstances(provisionInstances);
      awsInfrastructureMapping.setServiceId(serviceId);
      awsInfrastructureMapping.setComputeProviderType(computeProviderType);
      awsInfrastructureMapping.setAwsInstanceFilter(awsInstanceFilter);
      awsInfrastructureMapping.setAutoScalingGroupName(autoScalingGroupName);
      awsInfrastructureMapping.setDeploymentType(deploymentType);
      awsInfrastructureMapping.setComputeProviderName(computeProviderName);
      awsInfrastructureMapping.setDisplayName(displayName);
      return awsInfrastructureMapping;
    }
  }
}
