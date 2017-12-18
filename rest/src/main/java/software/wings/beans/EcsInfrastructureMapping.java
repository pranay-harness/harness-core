package software.wings.beans;

import static com.amazonaws.util.StringUtils.isNullOrEmpty;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.AwsInfrastructureMapping.AwsRegionDataProvider;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.utils.Util;

import java.util.List;
import java.util.Optional;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeName("AWS_ECS")
public class EcsInfrastructureMapping extends ContainerInfrastructureMapping {
  @Attributes(title = "Region")
  @DefaultValue("us-east-1")
  @EnumData(enumDataProvider = AwsRegionDataProvider.class)
  private String region;

  @SchemaIgnore private String vpc;

  @SchemaIgnore private List<String> subnet;

  @SchemaIgnore private String securityGroup;

  @SchemaIgnore private String type;

  @SchemaIgnore private String role;

  @SchemaIgnore private int diskSize;

  @SchemaIgnore private String ami;

  @SchemaIgnore private int numberOfNodes;

  /**
   * Instantiates a new Infrastructure mapping.
   */
  public EcsInfrastructureMapping() {
    super(InfrastructureMappingType.AWS_ECS.name());
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Util.normalize(String.format("%s (%s_%s::%s) %s", this.getClusterName(), this.getComputeProviderType(),
        this.getDeploymentType(),
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
        this.getRegion()));
  }

  /**
   * Getter for property 'region'.
   *
   * @return Value for property 'region'.
   */
  public String getRegion() {
    return isNullOrEmpty(region) ? "us-east-1" : region;
  }

  /**
   * Setter for property 'region'.
   *
   * @param region Value to set for property 'region'.
   */
  public void setRegion(String region) {
    this.region = region;
  }

  /**
   * Getter for property 'vpc'.
   *
   * @return Value for property 'vpc'.
   */
  public String getVpc() {
    return vpc;
  }

  /**
   * Setter for property 'vpc'.
   *
   * @param vpc Value to set for property 'vpc'.
   */
  public void setVpc(String vpc) {
    this.vpc = vpc;
  }

  /**
   * Getter for property 'subnet'.
   *
   * @return Value for property 'subnet'.
   */
  public List<String> getSubnet() {
    return subnet;
  }

  /**
   * Setter for property 'subnet'.
   *
   * @param subnet Value to set for property 'subnet'.
   */
  public void setSubnet(List<String> subnet) {
    this.subnet = subnet;
  }

  /**
   * Getter for property 'securityGroup'.
   *
   * @return Value for property 'securityGroup'.
   */
  public String getSecurityGroup() {
    return securityGroup;
  }

  /**
   * Setter for property 'securityGroup'.
   *
   * @param securityGroup Value to set for property 'securityGroup'.
   */
  public void setSecurityGroup(String securityGroup) {
    this.securityGroup = securityGroup;
  }

  /**
   * Getter for property 'type'.
   *
   * @return Value for property 'type'.
   */
  public String getType() {
    return type;
  }

  /**
   * Setter for property 'type'.
   *
   * @param type Value to set for property 'type'.
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Getter for property 'role'.
   *
   * @return Value for property 'role'.
   */
  public String getRole() {
    return role;
  }

  /**
   * Setter for property 'role'.
   *
   * @param role Value to set for property 'role'.
   */
  public void setRole(String role) {
    this.role = role;
  }

  /**
   * Getter for property 'diskSize'.
   *
   * @return Value for property 'diskSize'.
   */
  public int getDiskSize() {
    return diskSize;
  }

  /**
   * Setter for property 'diskSize'.
   *
   * @param diskSize Value to set for property 'diskSize'.
   */
  public void setDiskSize(int diskSize) {
    this.diskSize = diskSize;
  }

  /**
   * Getter for property 'ami'.
   *
   * @return Value for property 'ami'.
   */
  public String getAmi() {
    return ami;
  }

  /**
   * Setter for property 'ami'.
   *
   * @param ami Value to set for property 'ami'.
   */
  public void setAmi(String ami) {
    this.ami = ami;
  }

  /**
   * Getter for property 'numberOfNodes'.
   *
   * @return Value for property 'numberOfNodes'.
   */
  public int getNumberOfNodes() {
    return numberOfNodes;
  }

  /**
   * Setter for property 'numberOfNodes'.
   *
   * @param numberOfNodes Value to set for property 'numberOfNodes'.
   */
  public void setNumberOfNodes(int numberOfNodes) {
    this.numberOfNodes = numberOfNodes;
  }

  public Builder deepClone() {
    return anEcsInfrastructureMapping()
        .withClusterName(getClusterName())
        .withRegion(getRegion())
        .withVpc(getVpc())
        .withSubnet(getSubnet())
        .withSecurityGroup(getSecurityGroup())
        .withType(getType())
        .withRole(getRole())
        .withDiskSize(getDiskSize())
        .withAmi(getAmi())
        .withNumberOfNodes(getNumberOfNodes())
        .withUuid(getUuid())
        .withComputeProviderSettingId(getComputeProviderSettingId())
        .withAppId(getAppId())
        .withEnvId(getEnvId())
        .withCreatedBy(getCreatedBy())
        .withServiceTemplateId(getServiceTemplateId())
        .withCreatedAt(getCreatedAt())
        .withLastUpdatedBy(getLastUpdatedBy())
        .withServiceId(getServiceId())
        .withLastUpdatedAt(getLastUpdatedAt())
        .withComputeProviderType(getComputeProviderType())
        .withInfraMappingType(getInfraMappingType())
        .withEntityYamlPath(entityYamlPath)
        .withDeploymentType(getDeploymentType())
        .withComputeProviderName(getComputeProviderName())
        .withName(getName());
  }

  public static final class Builder {
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    private String accountId;
    private String clusterName;
    private String region;
    private String vpc;
    private List<String> subnet;
    private String securityGroup;
    private String type;
    private String role;
    private int diskSize;
    private String ami;
    private int numberOfNodes;
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

    public static Builder anEcsInfrastructureMapping() {
      return new Builder();
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withRegion(String region) {
      this.region = region;
      return this;
    }

    public Builder withVpc(String vpc) {
      this.vpc = vpc;
      return this;
    }

    public Builder withSubnet(List<String> subnet) {
      this.subnet = subnet;
      return this;
    }

    public Builder withSecurityGroup(String securityGroup) {
      this.securityGroup = securityGroup;
      return this;
    }

    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    public Builder withRole(String role) {
      this.role = role;
      return this;
    }

    public Builder withDiskSize(int diskSize) {
      this.diskSize = diskSize;
      return this;
    }

    public Builder withAmi(String ami) {
      this.ami = ami;
      return this;
    }

    public Builder withNumberOfNodes(int numberOfNodes) {
      this.numberOfNodes = numberOfNodes;
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
      return anEcsInfrastructureMapping()
          .withClusterName(clusterName)
          .withRegion(region)
          .withVpc(vpc)
          .withSubnet(subnet)
          .withSecurityGroup(securityGroup)
          .withType(type)
          .withRole(role)
          .withDiskSize(diskSize)
          .withAmi(ami)
          .withNumberOfNodes(numberOfNodes)
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

    public EcsInfrastructureMapping build() {
      EcsInfrastructureMapping ecsInfrastructureMapping = new EcsInfrastructureMapping();
      ecsInfrastructureMapping.setClusterName(clusterName);
      ecsInfrastructureMapping.setRegion(region);
      ecsInfrastructureMapping.setVpc(vpc);
      ecsInfrastructureMapping.setSubnet(subnet);
      ecsInfrastructureMapping.setSecurityGroup(securityGroup);
      ecsInfrastructureMapping.setType(type);
      ecsInfrastructureMapping.setRole(role);
      ecsInfrastructureMapping.setDiskSize(diskSize);
      ecsInfrastructureMapping.setAmi(ami);
      ecsInfrastructureMapping.setNumberOfNodes(numberOfNodes);
      ecsInfrastructureMapping.setUuid(uuid);
      ecsInfrastructureMapping.setAppId(appId);
      ecsInfrastructureMapping.setCreatedBy(createdBy);
      ecsInfrastructureMapping.setCreatedAt(createdAt);
      ecsInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      ecsInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      ecsInfrastructureMapping.setEntityYamlPath(entityYamlPath);
      ecsInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      ecsInfrastructureMapping.setEnvId(envId);
      ecsInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      ecsInfrastructureMapping.setServiceId(serviceId);
      ecsInfrastructureMapping.setComputeProviderType(computeProviderType);
      ecsInfrastructureMapping.setInfraMappingType(infraMappingType);
      ecsInfrastructureMapping.setDeploymentType(deploymentType);
      ecsInfrastructureMapping.setComputeProviderName(computeProviderName);
      ecsInfrastructureMapping.setName(name);
      ecsInfrastructureMapping.setAutoPopulate(autoPopulate);
      ecsInfrastructureMapping.setAccountId(accountId);
      return ecsInfrastructureMapping;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends ContainerInfrastructureMapping.Yaml {
    private String region = "us-east-1";

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String cluster, String region) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName, cluster);
      this.region = region;
    }
  }
}
