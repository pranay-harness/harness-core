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
 * Created by brett on 6/22/17
 */
@JsonTypeName("AWS_CODEDEPLOY")
public class CodeDeployInfrastructureMapping extends InfrastructureMapping {
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

  /**
   * Instantiates a new Aws CodeDeploy infrastructure mapping.
   */
  public CodeDeployInfrastructureMapping() {
    super(InfrastructureMappingType.AWS_CODEDEPLOY.name());
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

  public String getLoadBalancerId() {
    return loadBalancerId;
  }

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
    return String.format("%s(%s/%s)",
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
        this.getComputeProviderType(), this.getDeploymentType());
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public void setHostConnectionAttrs(String hostConnectionAttrs) {
    this.hostConnectionAttrs = hostConnectionAttrs;
  }

  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
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
    private String restrictionType;
    private String restrictionExpression;
    private String computeProviderSettingId;
    private String envId;
    private String serviceTemplateId;
    private String serviceId;
    private String computeProviderType;
    private String deploymentType;
    private String hostConnectionAttrs;
    private String computeProviderName;
    private String region;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * An aws infrastructure mapping builder.
     *
     * @return the builder
     */
    public static Builder aCodeDeployInfrastructureMapping() {
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
     * With compute provider name name builder.
     *
     * @param computeProviderName the display name
     * @return the builder
     */
    public Builder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aCodeDeployInfrastructureMapping()
          .withRestrictionType(restrictionType)
          .withRestrictionExpression(restrictionExpression)
          .withComputeProviderSettingId(computeProviderSettingId)
          .withEnvId(envId)
          .withServiceTemplateId(serviceTemplateId)
          .withServiceId(serviceId)
          .withComputeProviderType(computeProviderType)
          .withDeploymentType(deploymentType)
          .withHostConnectionAttrs(hostConnectionAttrs)
          .withComputeProviderName(computeProviderName)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withRegion(region)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build aws infrastructure mapping.
     *
     * @return the aws infrastructure mapping
     */
    public CodeDeployInfrastructureMapping build() {
      CodeDeployInfrastructureMapping awsInfrastructureMapping = new CodeDeployInfrastructureMapping();
      awsInfrastructureMapping.setRestrictionType(restrictionType);
      awsInfrastructureMapping.setRestrictionExpression(restrictionExpression);
      awsInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      awsInfrastructureMapping.setEnvId(envId);
      awsInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      awsInfrastructureMapping.setServiceId(serviceId);
      awsInfrastructureMapping.setComputeProviderType(computeProviderType);
      awsInfrastructureMapping.setDeploymentType(deploymentType);
      awsInfrastructureMapping.setHostConnectionAttrs(hostConnectionAttrs);
      awsInfrastructureMapping.setComputeProviderName(computeProviderName);
      awsInfrastructureMapping.setRegion(region);
      awsInfrastructureMapping.setUuid(uuid);
      awsInfrastructureMapping.setAppId(appId);
      awsInfrastructureMapping.setCreatedBy(createdBy);
      awsInfrastructureMapping.setCreatedAt(createdAt);
      awsInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      awsInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      return awsInfrastructureMapping;
    }
  }
}
