package software.wings.beans;

import static java.util.stream.Collectors.toMap;

import com.google.common.collect.ImmutableMap;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.app.MainConfiguration;
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

  @Attributes(title = "Instance Filter") private AwsInstanceFilter awsInstanceFilter;

  /**
   * Instantiates a new Aws infrastructure mapping.
   */
  public AwsInfrastructureMapping() {
    super(InfrastructureMappingType.AWS_SSH.name());
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
   * The type Builder.
   */
  public static final class Builder {
    private AwsInfrastructureMapping awsInfrastructureMapping;

    private Builder() {
      awsInfrastructureMapping = new AwsInfrastructureMapping();
    }

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
      awsInfrastructureMapping.setRestrictionType(restrictionType);
      return this;
    }

    /**
     * With restriction expression builder.
     *
     * @param restrictionExpression the restriction expression
     * @return the builder
     */
    public Builder withRestrictionExpression(String restrictionExpression) {
      awsInfrastructureMapping.setRestrictionExpression(restrictionExpression);
      return this;
    }

    /**
     * With region builder.
     *
     * @param region the region
     * @return the builder
     */
    public Builder withRegion(String region) {
      awsInfrastructureMapping.setRegion(region);
      return this;
    }

    /**
     * With host connection attrs builder.
     *
     * @param hostConnectionAttrs the host connection attrs
     * @return the builder
     */
    public Builder withHostConnectionAttrs(String hostConnectionAttrs) {
      awsInfrastructureMapping.setHostConnectionAttrs(hostConnectionAttrs);
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      awsInfrastructureMapping.setUuid(uuid);
      return this;
    }

    /**
     * With load balancer id builder.
     *
     * @param loadBalancerId the load balancer id
     * @return the builder
     */
    public Builder withLoadBalancerId(String loadBalancerId) {
      awsInfrastructureMapping.setLoadBalancerId(loadBalancerId);
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      awsInfrastructureMapping.setAppId(appId);
      return this;
    }

    /**
     * With load balancer name builder.
     *
     * @param loadBalancerName the load balancer name
     * @return the builder
     */
    public Builder withLoadBalancerName(String loadBalancerName) {
      awsInfrastructureMapping.setLoadBalancerName(loadBalancerName);
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      awsInfrastructureMapping.setCreatedBy(createdBy);
      return this;
    }

    /**
     * With aws instance filter builder.
     *
     * @param awsInstanceFilter the aws instance filter
     * @return the builder
     */
    public Builder withAwsInstanceFilter(AwsInstanceFilter awsInstanceFilter) {
      awsInfrastructureMapping.setAwsInstanceFilter(awsInstanceFilter);
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      awsInfrastructureMapping.setCreatedAt(createdAt);
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      awsInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      return this;
    }

    /**
     * With compute provider setting id builder.
     *
     * @param computeProviderSettingId the compute provider setting id
     * @return the builder
     */
    public Builder withComputeProviderSettingId(String computeProviderSettingId) {
      awsInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      awsInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      return this;
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      awsInfrastructureMapping.setEnvId(envId);
      return this;
    }

    /**
     * With service template id builder.
     *
     * @param serviceTemplateId the service template id
     * @return the builder
     */
    public Builder withServiceTemplateId(String serviceTemplateId) {
      awsInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      return this;
    }

    /**
     * With service id builder.
     *
     * @param serviceId the service id
     * @return the builder
     */
    public Builder withServiceId(String serviceId) {
      awsInfrastructureMapping.setServiceId(serviceId);
      return this;
    }

    /**
     * With compute provider type builder.
     *
     * @param computeProviderType the compute provider type
     * @return the builder
     */
    public Builder withComputeProviderType(String computeProviderType) {
      awsInfrastructureMapping.setComputeProviderType(computeProviderType);
      return this;
    }

    /**
     * With deployment type builder.
     *
     * @param deploymentType the deployment type
     * @return the builder
     */
    public Builder withDeploymentType(String deploymentType) {
      awsInfrastructureMapping.setDeploymentType(deploymentType);
      return this;
    }

    /**
     * With compute provider name builder.
     *
     * @param computeProviderName the compute provider name
     * @return the builder
     */
    public Builder withComputeProviderName(String computeProviderName) {
      awsInfrastructureMapping.setComputeProviderName(computeProviderName);
      return this;
    }

    /**
     * With display name builder.
     *
     * @param displayName the display name
     * @return the builder
     */
    public Builder withDisplayName(String displayName) {
      awsInfrastructureMapping.setDisplayName(displayName);
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anAwsInfrastructureMapping()
          .withRestrictionType(awsInfrastructureMapping.getRestrictionType())
          .withRestrictionExpression(awsInfrastructureMapping.getRestrictionExpression())
          .withRegion(awsInfrastructureMapping.getRegion())
          .withHostConnectionAttrs(awsInfrastructureMapping.getHostConnectionAttrs())
          .withUuid(awsInfrastructureMapping.getUuid())
          .withLoadBalancerId(awsInfrastructureMapping.getLoadBalancerId())
          .withAppId(awsInfrastructureMapping.getAppId())
          .withLoadBalancerName(awsInfrastructureMapping.getLoadBalancerName())
          .withCreatedBy(awsInfrastructureMapping.getCreatedBy())
          .withAwsInstanceFilter(awsInfrastructureMapping.getAwsInstanceFilter())
          .withCreatedAt(awsInfrastructureMapping.getCreatedAt())
          .withLastUpdatedBy(awsInfrastructureMapping.getLastUpdatedBy())
          .withComputeProviderSettingId(awsInfrastructureMapping.getComputeProviderSettingId())
          .withLastUpdatedAt(awsInfrastructureMapping.getLastUpdatedAt())
          .withEnvId(awsInfrastructureMapping.getEnvId())
          .withServiceTemplateId(awsInfrastructureMapping.getServiceTemplateId())
          .withServiceId(awsInfrastructureMapping.getServiceId())
          .withComputeProviderType(awsInfrastructureMapping.getComputeProviderType())
          .withDeploymentType(awsInfrastructureMapping.getDeploymentType())
          .withComputeProviderName(awsInfrastructureMapping.getComputeProviderName())
          .withDisplayName(awsInfrastructureMapping.getDisplayName());
    }

    /**
     * Build aws infrastructure mapping.
     *
     * @return the aws infrastructure mapping
     */
    public AwsInfrastructureMapping build() {
      return awsInfrastructureMapping;
    }
  }
}
