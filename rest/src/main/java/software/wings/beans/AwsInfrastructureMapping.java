package software.wings.beans;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;

import com.google.common.collect.ImmutableMap;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.app.MainConfiguration;
import software.wings.exception.WingsException;
import software.wings.stencils.DataProvider;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.utils.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

  @Deprecated @SchemaIgnore private String customName;

  @Attributes(title = "Use Public DNS for SSH connection") private boolean usePublicDns;

  @Attributes(title = "Use Auto Scaling group") private boolean provisionInstances;

  @Attributes(title = "Instance Filter") private AwsInstanceFilter awsInstanceFilter;

  @Attributes(title = "Auto Scaling group") private String autoScalingGroupName;

  @Attributes(title = "Set Auto Scaling group desired capacity") private boolean setDesiredCapacity;

  @Attributes(title = "Desired Capacity") private int desiredCapacity;

  /**
   * Instantiates a new Aws infrastructure mapping.
   */
  public AwsInfrastructureMapping() {
    super(InfrastructureMappingType.AWS_SSH.name());
  }

  /**
   * The type Yaml.
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends InfrastructureMapping.Yaml {
    // maps to restrictionType
    private String restrictions;
    // maps to restrictionExpression
    private String expression;
    private String region = "us-east-1";
    // maps to hostConnectionAttrs
    private String connectionType;
    private String loadBalancer;
    private boolean usePublicDns;
    private boolean provisionInstances;
    private String autoScalingGroup;
    private int desiredCapacity;

    // These four fields map to AwsInstanceFilter
    private List<String> vpcs = new ArrayList<>();
    private List<String> subnetIds = new ArrayList<>();
    private List<String> securityGroupIds = new ArrayList<>();
    private List<NameValuePair.Yaml> tags = new ArrayList<>();

    /**
     * The type Builder.
     */
    public static final class Builder {
      private String computeProviderType;
      // maps to restrictionType
      private String restrictions;
      private String serviceName;
      // maps to restrictionExpression
      private String expression;
      private String infraMappingType;
      private String type;
      private String region = "us-east-1";
      private String deploymentType;
      private String computeProviderName;
      // maps to hostConnectionAttrs
      private String connectionType;
      private String name;
      private String loadBalancer;
      private boolean usePublicDns;
      private boolean provisionInstances;
      private String autoScalingGroup;
      private int desiredCapacity;
      // These four fields map to AwsInstanceFilter
      private List<String> vpcs = new ArrayList<>();
      private List<String> subnetIds = new ArrayList<>();
      private List<String> securityGroupIds = new ArrayList<>();
      private List<NameValuePair.Yaml> tags = new ArrayList<>();

      private Builder() {}

      /**
       * A yaml builder.
       *
       * @return the builder
       */
      public static Builder aYaml() {
        return new Builder();
      }

      /**
       * With compute provider type builder.
       *
       * @param computeProviderType the compute provider type
       * @return the builder
       */
      public Builder withComputeProviderType(String computeProviderType) {
        this.computeProviderType = computeProviderType;
        return this;
      }

      /**
       * With restrictions builder.
       *
       * @param restrictions the restrictions
       * @return the builder
       */
      public Builder withRestrictions(String restrictions) {
        this.restrictions = restrictions;
        return this;
      }

      /**
       * With service name builder.
       *
       * @param serviceName the service name
       * @return the builder
       */
      public Builder withServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
      }

      /**
       * With expression builder.
       *
       * @param expression the expression
       * @return the builder
       */
      public Builder withExpression(String expression) {
        this.expression = expression;
        return this;
      }

      /**
       * With infra mapping type builder.
       *
       * @param infraMappingType the infra mapping type
       * @return the builder
       */
      public Builder withInfraMappingType(String infraMappingType) {
        this.infraMappingType = infraMappingType;
        return this;
      }

      /**
       * With type builder.
       *
       * @param type the type
       * @return the builder
       */
      public Builder withType(String type) {
        this.type = type;
        return this;
      }

      /**
       * With region builder.
       *
       * @param region the region
       * @return the builder
       */
      public Builder withRegion(String region) {
        this.region = region;
        return this;
      }

      /**
       * With deployment type builder.
       *
       * @param deploymentType the deployment type
       * @return the builder
       */
      public Builder withDeploymentType(String deploymentType) {
        this.deploymentType = deploymentType;
        return this;
      }

      /**
       * With compute provider name builder.
       *
       * @param computeProviderName the compute provider name
       * @return the builder
       */
      public Builder withComputeProviderName(String computeProviderName) {
        this.computeProviderName = computeProviderName;
        return this;
      }

      /**
       * With connection type builder.
       *
       * @param connectionType the connection type
       * @return the builder
       */
      public Builder withConnectionType(String connectionType) {
        this.connectionType = connectionType;
        return this;
      }

      /**
       * With name builder.
       *
       * @param name the name
       * @return the builder
       */
      public Builder withName(String name) {
        this.name = name;
        return this;
      }

      /**
       * With load balancer builder.
       *
       * @param loadBalancer the load balancer
       * @return the builder
       */
      public Builder withLoadBalancer(String loadBalancer) {
        this.loadBalancer = loadBalancer;
        return this;
      }

      /**
       * With use public dns builder.
       *
       * @param usePublicDns the use public dns
       * @return the builder
       */
      public Builder withUsePublicDns(boolean usePublicDns) {
        this.usePublicDns = usePublicDns;
        return this;
      }

      /**
       * With provision instances builder.
       *
       * @param provisionInstances the provision instances
       * @return the builder
       */
      public Builder withProvisionInstances(boolean provisionInstances) {
        this.provisionInstances = provisionInstances;
        return this;
      }

      /**
       * With auto scaling group builder.
       *
       * @param autoScalingGroup the auto scaling group
       * @return the builder
       */
      public Builder withAutoScalingGroup(String autoScalingGroup) {
        this.autoScalingGroup = autoScalingGroup;
        return this;
      }

      /**
       * With desired capacity builder.
       *
       * @param desiredCapacity the desired capacity
       * @return the builder
       */
      public Builder withDesiredCapacity(int desiredCapacity) {
        this.desiredCapacity = desiredCapacity;
        return this;
      }

      /**
       * With vpcs builder.
       *
       * @param vpcs the vpcs
       * @return the builder
       */
      public Builder withVpcs(List<String> vpcs) {
        this.vpcs = vpcs;
        return this;
      }

      /**
       * With subnet ids builder.
       *
       * @param subnetIds the subnet ids
       * @return the builder
       */
      public Builder withSubnetIds(List<String> subnetIds) {
        this.subnetIds = subnetIds;
        return this;
      }

      /**
       * With security group ids builder.
       *
       * @param securityGroupIds the security group ids
       * @return the builder
       */
      public Builder withSecurityGroupIds(List<String> securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
        return this;
      }

      /**
       * With tags builder.
       *
       * @param tags the tags
       * @return the builder
       */
      public Builder withTags(List<NameValuePair.Yaml> tags) {
        this.tags = tags;
        return this;
      }

      /**
       * But builder.
       *
       * @return the builder
       */
      public Builder but() {
        return aYaml()
            .withComputeProviderType(computeProviderType)
            .withRestrictions(restrictions)
            .withServiceName(serviceName)
            .withExpression(expression)
            .withInfraMappingType(infraMappingType)
            .withType(type)
            .withRegion(region)
            .withDeploymentType(deploymentType)
            .withComputeProviderName(computeProviderName)
            .withConnectionType(connectionType)
            .withName(name)
            .withLoadBalancer(loadBalancer)
            .withUsePublicDns(usePublicDns)
            .withProvisionInstances(provisionInstances)
            .withAutoScalingGroup(autoScalingGroup)
            .withDesiredCapacity(desiredCapacity)
            .withVpcs(vpcs)
            .withSubnetIds(subnetIds)
            .withSecurityGroupIds(securityGroupIds)
            .withTags(tags);
      }

      /**
       * Build yaml.
       *
       * @return the yaml
       */
      public Yaml build() {
        Yaml yaml = new Yaml();
        yaml.setComputeProviderType(computeProviderType);
        yaml.setRestrictions(restrictions);
        yaml.setServiceName(serviceName);
        yaml.setExpression(expression);
        yaml.setInfraMappingType(infraMappingType);
        yaml.setType(type);
        yaml.setRegion(region);
        yaml.setDeploymentType(deploymentType);
        yaml.setComputeProviderName(computeProviderName);
        yaml.setConnectionType(connectionType);
        yaml.setName(name);
        yaml.setLoadBalancer(loadBalancer);
        yaml.setUsePublicDns(usePublicDns);
        yaml.setProvisionInstances(provisionInstances);
        yaml.setAutoScalingGroup(autoScalingGroup);
        yaml.setDesiredCapacity(desiredCapacity);
        yaml.setVpcs(vpcs);
        yaml.setSubnetIds(subnetIds);
        yaml.setSecurityGroupIds(securityGroupIds);
        yaml.setTags(tags);
        return yaml;
      }
    }
  }

  /**
   * Validate.
   */
  public void validate() {
    if (provisionInstances) {
      if (isEmpty(autoScalingGroupName)) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args",
            "Auto Scaling group must not be empty when provision instances is true.");
      }
      if (setDesiredCapacity && desiredCapacity <= 0) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", "Desired count must be greater than zero.");
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
  public String getDefaultName() {
    return Util.normalize(String.format("%s%s (AWS_SSH) %s", Util.isNotEmpty(customName) ? (customName + " - ") : "",
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
        this.getRegion()));
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

  /**
   * Is provision instances boolean.
   *
   * @return the boolean
   */
  public boolean isProvisionInstances() {
    return provisionInstances;
  }

  /**
   * Sets provision instances.
   *
   * @param provisionInstances the provision instances
   */
  public void setProvisionInstances(boolean provisionInstances) {
    this.provisionInstances = provisionInstances;
  }

  /**
   * Is use public dns boolean.
   *
   * @return the boolean
   */
  public boolean isUsePublicDns() {
    return usePublicDns;
  }

  /**
   * Sets use public dns.
   *
   * @param usePublicDns the use public dns
   */
  public void setUsePublicDns(boolean usePublicDns) {
    this.usePublicDns = usePublicDns;
  }

  /**
   * Gets auto scaling group name.
   *
   * @return the auto scaling group name
   */
  public String getAutoScalingGroupName() {
    return autoScalingGroupName;
  }

  /**
   * Sets auto scaling group name.
   *
   * @param autoScalingGroupName the auto scaling group name
   */
  public void setAutoScalingGroupName(String autoScalingGroupName) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  /**
   * Gets custom name.
   *
   * @return the custom name
   */
  @SchemaIgnore
  public String getCustomName() {
    return customName;
  }

  /**
   * Sets custom name.
   *
   * @param customName the custom name
   */
  public void setCustomName(String customName) {
    this.customName = customName;
  }

  /**
   * Is set desired capacity boolean.
   *
   * @return the boolean
   */
  public boolean isSetDesiredCapacity() {
    return setDesiredCapacity;
  }

  /**
   * Sets set desired capacity.
   *
   * @param setDesiredCapacity the set desired capacity
   */
  public void setSetDesiredCapacity(boolean setDesiredCapacity) {
    this.setDesiredCapacity = setDesiredCapacity;
  }

  /**
   * Gets desired capacity.
   *
   * @return the desired capacity
   */
  public int getDesiredCapacity() {
    return desiredCapacity;
  }

  /**
   * Sets desired capacity.
   *
   * @param desiredCapacity the desired capacity
   */
  public void setDesiredCapacity(int desiredCapacity) {
    this.desiredCapacity = desiredCapacity;
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

  /**
   * Clone and return builder.
   *
   * @return the builder
   */
  public Builder deepClone() {
    return anAwsInfrastructureMapping()
        .withRestrictionType(getRestrictionType())
        .withRestrictionExpression(getRestrictionExpression())
        .withRegion(getRegion())
        .withUuid(getUuid())
        .withAppId(getAppId())
        .withHostConnectionAttrs(getHostConnectionAttrs())
        .withCreatedBy(getCreatedBy())
        .withLoadBalancerId(getLoadBalancerId())
        .withCreatedAt(getCreatedAt())
        .withLoadBalancerName(getLoadBalancerName())
        .withLastUpdatedBy(getLastUpdatedBy())
        .withComputeProviderSettingId(getComputeProviderSettingId())
        .withLastUpdatedAt(getLastUpdatedAt())
        .withEnvId(getEnvId())
        .withUsePublicDns(isUsePublicDns())
        .withServiceTemplateId(getServiceTemplateId())
        .withProvisionInstances(isProvisionInstances())
        .withServiceId(getServiceId())
        .withComputeProviderType(getComputeProviderType())
        .withAwsInstanceFilter(getAwsInstanceFilter())
        .withInfraMappingType(getInfraMappingType())
        .withAutoScalingGroupName(getAutoScalingGroupName())
        .withDeploymentType(getDeploymentType())
        .withComputeProviderName(getComputeProviderName())
        .withName(getName())
        .withSetDesiredCapacity(isSetDesiredCapacity());
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    /**
     * The Entity yaml path.
     */
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    /**
     * The App id.
     */
    protected String appId;
    private String accountId;
    private String restrictionType;
    private String restrictionExpression;
    private String region;
    private String uuid;
    private EmbeddedUser createdBy;
    private String hostConnectionAttrs;
    private long createdAt;
    private String loadBalancerId;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private String loadBalancerName;
    private String customName;
    private boolean usePublicDns;
    private String computeProviderSettingId;
    private boolean provisionInstances;
    private String envId;
    private String serviceTemplateId;
    private AwsInstanceFilter awsInstanceFilter;
    private String serviceId;
    private String autoScalingGroupName;
    private String computeProviderType;
    private String infraMappingType;
    private boolean setDesiredCapacity;
    private String deploymentType;
    private int desiredCapacity;
    private String computeProviderName;
    private String name;
    // auto populate name
    private boolean autoPopulate = true;

    private Builder() {}

    /**
     * An aws infrastructure mapping builder.
     *
     * @return the builder
     */
    public static Builder anAwsInfrastructureMapping() {
      return new Builder();
    }

    /**
     * With restriction type builder.
     *
     * @param restrictionType the restriction type
     * @return the builder
     */
    public Builder withRestrictionType(String restrictionType) {
      this.restrictionType = restrictionType;
      return this;
    }

    /**
     * With restriction expression builder.
     *
     * @param restrictionExpression the restriction expression
     * @return the builder
     */
    public Builder withRestrictionExpression(String restrictionExpression) {
      this.restrictionExpression = restrictionExpression;
      return this;
    }

    /**
     * With region builder.
     *
     * @param region the region
     * @return the builder
     */
    public Builder withRegion(String region) {
      this.region = region;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With host connection attrs builder.
     *
     * @param hostConnectionAttrs the host connection attrs
     * @return the builder
     */
    public Builder withHostConnectionAttrs(String hostConnectionAttrs) {
      this.hostConnectionAttrs = hostConnectionAttrs;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With load balancer id builder.
     *
     * @param loadBalancerId the load balancer id
     * @return the builder
     */
    public Builder withLoadBalancerId(String loadBalancerId) {
      this.loadBalancerId = loadBalancerId;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With load balancer name builder.
     *
     * @param loadBalancerName the load balancer name
     * @return the builder
     */
    public Builder withLoadBalancerName(String loadBalancerName) {
      this.loadBalancerName = loadBalancerName;
      return this;
    }

    /**
     * With custom name builder.
     *
     * @param customName the custom name
     * @return the builder
     */
    public Builder withCustomName(String customName) {
      this.customName = customName;
      return this;
    }

    /**
     * With entity yaml path builder.
     *
     * @param entityYamlPath the entity yaml path
     * @return the builder
     */
    public Builder withEntityYamlPath(String entityYamlPath) {
      this.entityYamlPath = entityYamlPath;
      return this;
    }

    /**
     * With use public dns builder.
     *
     * @param usePublicDns the use public dns
     * @return the builder
     */
    public Builder withUsePublicDns(boolean usePublicDns) {
      this.usePublicDns = usePublicDns;
      return this;
    }

    /**
     * With compute provider setting id builder.
     *
     * @param computeProviderSettingId the compute provider setting id
     * @return the builder
     */
    public Builder withComputeProviderSettingId(String computeProviderSettingId) {
      this.computeProviderSettingId = computeProviderSettingId;
      return this;
    }

    /**
     * With provision instances builder.
     *
     * @param provisionInstances the provision instances
     * @return the builder
     */
    public Builder withProvisionInstances(boolean provisionInstances) {
      this.provisionInstances = provisionInstances;
      return this;
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With service template id builder.
     *
     * @param serviceTemplateId the service template id
     * @return the builder
     */
    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    /**
     * With aws instance filter builder.
     *
     * @param awsInstanceFilter the aws instance filter
     * @return the builder
     */
    public Builder withAwsInstanceFilter(AwsInstanceFilter awsInstanceFilter) {
      this.awsInstanceFilter = awsInstanceFilter;
      return this;
    }

    /**
     * With service id builder.
     *
     * @param serviceId the service id
     * @return the builder
     */
    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * With auto scaling group name builder.
     *
     * @param autoScalingGroupName the auto scaling group name
     * @return the builder
     */
    public Builder withAutoScalingGroupName(String autoScalingGroupName) {
      this.autoScalingGroupName = autoScalingGroupName;
      return this;
    }

    /**
     * With compute provider type builder.
     *
     * @param computeProviderType the compute provider type
     * @return the builder
     */
    public Builder withComputeProviderType(String computeProviderType) {
      this.computeProviderType = computeProviderType;
      return this;
    }

    /**
     * With infra mapping type builder.
     *
     * @param infraMappingType the infra mapping type
     * @return the builder
     */
    public Builder withInfraMappingType(String infraMappingType) {
      this.infraMappingType = infraMappingType;
      return this;
    }

    /**
     * With set desired capacity builder.
     *
     * @param setDesiredCapacity the set desired capacity
     * @return the builder
     */
    public Builder withSetDesiredCapacity(boolean setDesiredCapacity) {
      this.setDesiredCapacity = setDesiredCapacity;
      return this;
    }

    /**
     * With deployment type builder.
     *
     * @param deploymentType the deployment type
     * @return the builder
     */
    public Builder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    /**
     * With desired capacity builder.
     *
     * @param desiredCapacity the desired capacity
     * @return the builder
     */
    public Builder withDesiredCapacity(int desiredCapacity) {
      this.desiredCapacity = desiredCapacity;
      return this;
    }

    /**
     * With compute provider name builder.
     *
     * @param computeProviderName the compute provider name
     * @return the builder
     */
    public Builder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With auto populate builder.
     *
     * @param autoPopulate the auto populate
     * @return the builder
     */
    public Builder withAutoPopulate(boolean autoPopulate) {
      this.autoPopulate = autoPopulate;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anAwsInfrastructureMapping()
          .withRestrictionType(restrictionType)
          .withRestrictionExpression(restrictionExpression)
          .withRegion(region)
          .withUuid(uuid)
          .withAppId(appId)
          .withAccountId(accountId)
          .withCreatedBy(createdBy)
          .withHostConnectionAttrs(hostConnectionAttrs)
          .withCreatedAt(createdAt)
          .withLoadBalancerId(loadBalancerId)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withLoadBalancerName(loadBalancerName)
          .withCustomName(customName)
          .withEntityYamlPath(entityYamlPath)
          .withUsePublicDns(usePublicDns)
          .withComputeProviderSettingId(computeProviderSettingId)
          .withProvisionInstances(provisionInstances)
          .withEnvId(envId)
          .withServiceTemplateId(serviceTemplateId)
          .withAwsInstanceFilter(awsInstanceFilter)
          .withServiceId(serviceId)
          .withAutoScalingGroupName(autoScalingGroupName)
          .withComputeProviderType(computeProviderType)
          .withInfraMappingType(infraMappingType)
          .withSetDesiredCapacity(setDesiredCapacity)
          .withDeploymentType(deploymentType)
          .withDesiredCapacity(desiredCapacity)
          .withComputeProviderName(computeProviderName)
          .withName(name)
          .withAutoPopulate(autoPopulate);
    }

    /**
     * Build aws infrastructure mapping.
     *
     * @return the aws infrastructure mapping
     */
    public AwsInfrastructureMapping build() {
      AwsInfrastructureMapping awsInfrastructureMapping = new AwsInfrastructureMapping();
      awsInfrastructureMapping.setRestrictionType(restrictionType);
      awsInfrastructureMapping.setRestrictionExpression(restrictionExpression);
      awsInfrastructureMapping.setRegion(region);
      awsInfrastructureMapping.setUuid(uuid);
      awsInfrastructureMapping.setAppId(appId);
      awsInfrastructureMapping.setCreatedBy(createdBy);
      awsInfrastructureMapping.setHostConnectionAttrs(hostConnectionAttrs);
      awsInfrastructureMapping.setCreatedAt(createdAt);
      awsInfrastructureMapping.setLoadBalancerId(loadBalancerId);
      awsInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      awsInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      awsInfrastructureMapping.setLoadBalancerName(loadBalancerName);
      awsInfrastructureMapping.setCustomName(customName);
      awsInfrastructureMapping.setEntityYamlPath(entityYamlPath);
      awsInfrastructureMapping.setUsePublicDns(usePublicDns);
      awsInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      awsInfrastructureMapping.setProvisionInstances(provisionInstances);
      awsInfrastructureMapping.setEnvId(envId);
      awsInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      awsInfrastructureMapping.setAwsInstanceFilter(awsInstanceFilter);
      awsInfrastructureMapping.setServiceId(serviceId);
      awsInfrastructureMapping.setAutoScalingGroupName(autoScalingGroupName);
      awsInfrastructureMapping.setComputeProviderType(computeProviderType);
      awsInfrastructureMapping.setInfraMappingType(infraMappingType);
      awsInfrastructureMapping.setSetDesiredCapacity(setDesiredCapacity);
      awsInfrastructureMapping.setDeploymentType(deploymentType);
      awsInfrastructureMapping.setDesiredCapacity(desiredCapacity);
      awsInfrastructureMapping.setComputeProviderName(computeProviderName);
      awsInfrastructureMapping.setName(name);
      awsInfrastructureMapping.setAutoPopulate(autoPopulate);
      awsInfrastructureMapping.setAccountId(accountId);
      return awsInfrastructureMapping;
    }
  }
}
