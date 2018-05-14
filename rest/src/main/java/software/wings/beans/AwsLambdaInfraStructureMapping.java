package software.wings.beans;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.stencils.EnumData;
import software.wings.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The type Aws lambda infra structure mapping.
 */
@JsonTypeName("AWS_AWS_LAMBDA")
public class AwsLambdaInfraStructureMapping extends InfrastructureMapping {
  /**
   * Instantiates a new Infrastructure mapping.
   */
  public AwsLambdaInfraStructureMapping() {
    super(InfrastructureMappingType.AWS_AWS_LAMBDA.name());
  }

  @Override
  public void applyProvisionerVariables(Map<String, Object> map) {}

  @Attributes(title = "Region", required = true)
  @NotEmpty
  @EnumData(enumDataProvider = AwsInfrastructureMapping.AwsRegionDataProvider.class)
  private String region;

  @Attributes(title = "VPC") private String vpcId;
  @Attributes(title = "Subnets") private List<String> subnetIds = new ArrayList<>();
  @Attributes(title = "Security Groups") private List<String> securityGroupIds = new ArrayList<>();
  @Attributes(title = " IAM role") @NotEmpty private String role;

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return null;
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Util.normalize(format("%s (AWS_Lambda) %s",
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
   * Gets vpc id.
   *
   * @return the vpc id
   */
  public String getVpcId() {
    return vpcId;
  }

  /**
   * Sets vpc id.
   *
   * @param vpcId the vpc id
   */
  public void setVpcId(String vpcId) {
    this.vpcId = vpcId;
  }

  /**
   * Gets subnet ids.
   *
   * @return the subnet ids
   */
  public List<String> getSubnetIds() {
    return subnetIds;
  }

  /**
   * Sets subnet ids.
   *
   * @param subnetIds the subnet ids
   */
  public void setSubnetIds(List<String> subnetIds) {
    this.subnetIds = subnetIds;
  }

  /**
   * Gets security group ids.
   *
   * @return the security group ids
   */
  public List<String> getSecurityGroupIds() {
    return securityGroupIds;
  }

  /**
   * Sets security group ids.
   *
   * @param securityGroupIds the security group ids
   */
  public void setSecurityGroupIds(List<String> securityGroupIds) {
    this.securityGroupIds = securityGroupIds;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public static final class Builder {
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    private String accountId;
    private String region;
    private String vpcId;
    private List<String> subnetIds = new ArrayList<>();
    private List<String> securityGroupIds = new ArrayList<>();
    private String role;
    private String uuid;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private String computeProviderSettingId;
    private String envId;
    private String serviceTemplateId;
    private String serviceId;
    private String computeProviderType;
    private String infraMappingType;
    private String deploymentType;
    private String computeProviderName;
    private String name;
    // auto populate name
    private boolean autoPopulate = true;

    private Builder() {}

    public static Builder anAwsLambdaInfraStructureMapping() {
      return new Builder();
    }

    public Builder withRegion(String region) {
      this.region = region;
      return this;
    }

    public Builder withVpcId(String vpcId) {
      this.vpcId = vpcId;
      return this;
    }

    public Builder withSubnetIds(List<String> subnetIds) {
      this.subnetIds = subnetIds;
      return this;
    }

    public Builder withSecurityGroupIds(List<String> securityGroupIds) {
      this.securityGroupIds = securityGroupIds;
      return this;
    }

    public Builder withRole(String role) {
      this.role = role;
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

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withEntityYamlPath(String entityYamlPath) {
      this.entityYamlPath = entityYamlPath;
      return this;
    }

    public Builder withComputeProviderSettingId(String computeProviderSettingId) {
      this.computeProviderSettingId = computeProviderSettingId;
      return this;
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
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

    public Builder withInfraMappingType(String infraMappingType) {
      this.infraMappingType = infraMappingType;
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

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withAutoPopulate(boolean autoPopulate) {
      this.autoPopulate = autoPopulate;
      return this;
    }

    public Builder but() {
      return anAwsLambdaInfraStructureMapping()
          .withRegion(region)
          .withVpcId(vpcId)
          .withSubnetIds(subnetIds)
          .withSecurityGroupIds(securityGroupIds)
          .withRole(role)
          .withUuid(uuid)
          .withAppId(appId)
          .withAccountId(accountId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withEntityYamlPath(entityYamlPath)
          .withComputeProviderSettingId(computeProviderSettingId)
          .withEnvId(envId)
          .withServiceTemplateId(serviceTemplateId)
          .withServiceId(serviceId)
          .withComputeProviderType(computeProviderType)
          .withInfraMappingType(infraMappingType)
          .withDeploymentType(deploymentType)
          .withComputeProviderName(computeProviderName)
          .withName(name)
          .withAutoPopulate(autoPopulate);
    }

    public AwsLambdaInfraStructureMapping build() {
      AwsLambdaInfraStructureMapping awsLambdaInfraStructureMapping = new AwsLambdaInfraStructureMapping();
      awsLambdaInfraStructureMapping.setRegion(region);
      awsLambdaInfraStructureMapping.setVpcId(vpcId);
      awsLambdaInfraStructureMapping.setSubnetIds(subnetIds);
      awsLambdaInfraStructureMapping.setSecurityGroupIds(securityGroupIds);
      awsLambdaInfraStructureMapping.setRole(role);
      awsLambdaInfraStructureMapping.setUuid(uuid);
      awsLambdaInfraStructureMapping.setAppId(appId);
      awsLambdaInfraStructureMapping.setCreatedBy(createdBy);
      awsLambdaInfraStructureMapping.setCreatedAt(createdAt);
      awsLambdaInfraStructureMapping.setLastUpdatedBy(lastUpdatedBy);
      awsLambdaInfraStructureMapping.setLastUpdatedAt(lastUpdatedAt);
      awsLambdaInfraStructureMapping.setEntityYamlPath(entityYamlPath);
      awsLambdaInfraStructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      awsLambdaInfraStructureMapping.setEnvId(envId);
      awsLambdaInfraStructureMapping.setServiceTemplateId(serviceTemplateId);
      awsLambdaInfraStructureMapping.setServiceId(serviceId);
      awsLambdaInfraStructureMapping.setComputeProviderType(computeProviderType);
      awsLambdaInfraStructureMapping.setInfraMappingType(infraMappingType);
      awsLambdaInfraStructureMapping.setDeploymentType(deploymentType);
      awsLambdaInfraStructureMapping.setComputeProviderName(computeProviderName);
      awsLambdaInfraStructureMapping.setName(name);
      awsLambdaInfraStructureMapping.setAutoPopulate(autoPopulate);
      awsLambdaInfraStructureMapping.setAccountId(accountId);
      return awsLambdaInfraStructureMapping;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends InfrastructureMapping.YamlWithComputeProvider {
    private String region;
    private String vpcId;
    private List<String> subnetIds = new ArrayList<>();
    private List<String> securityGroupIds = new ArrayList<>();
    private String role;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String region, String vpcId,
        List<String> subnetIds, List<String> securityGroupIds, String role) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName);
      this.region = region;
      this.vpcId = vpcId;
      this.subnetIds = subnetIds;
      this.securityGroupIds = securityGroupIds;
      this.role = role;
    }
  }
}
