/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType.AWS_ECS_FARGATE;
import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;

import static com.amazonaws.util.StringUtils.isNullOrEmpty;
import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.annotation.Blueprint;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.utils.Utils;

import com.amazonaws.services.ecs.model.LaunchType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeName("AWS_ECS")
@FieldNameConstants(innerTypeName = "EcsInfrastructureMappingKeys")
@OwnedBy(CDP)
@TargetModule(_957_CG_BEANS)
public class EcsInfrastructureMapping extends ContainerInfrastructureMapping {
  @Attributes(title = "Region")
  @DefaultValue(AWS_DEFAULT_REGION)
  @EnumData(enumDataProvider = AwsRegionDataProvider.class)
  @Blueprint
  private String region;
  @Blueprint private String vpcId;
  @Blueprint private List<String> subnetIds;
  @Blueprint private List<String> securityGroupIds;
  private boolean assignPublicIp;
  @Blueprint private String executionRole;
  private String launchType;

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

  @lombok.Builder
  public EcsInfrastructureMapping(String accountId, String region, String clusterName) {
    this();
    this.accountId = accountId;
    this.region = region;
    super.setClusterName(clusterName);
  }

  private boolean applyCommonVariable(String key, Object value) {
    switch (key) {
      case "region": {
        setRegion((String) value);
        return true;
      }
      case "ecsCluster": {
        setClusterName((String) value);
        return true;
      }
      case "ecsVpc": {
        setVpcId((String) value);
        return true;
      }
      case "ecsSgs": {
        setSecurityGroupIds(getList(value));
        return true;
      }
      case "ecsSubnets": {
        setSubnetIds(getList(value));
        return true;
      }
      default: {
        return false;
      }
    }
  }

  private void applyEcsFargateVariables(Map<String, Object> map) {
    setLaunchType("FARGATE");
    try {
      for (Entry<String, Object> entry : map.entrySet()) {
        if (!applyCommonVariable(entry.getKey(), entry.getValue())) {
          if ("ecsTaskExecutionRole".equals(entry.getKey())) {
            setExecutionRole((String) entry.getValue());
          } else {
            throw new InvalidRequestException(
                format("Unsupported infrastructure mapping provisioner variable %s", entry.getKey()));
          }
        }
      }
    } catch (WingsException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new InvalidRequestException("Unable to set the provisioner variables to the mapping", exception);
    }
  }

  private void applyEcsEc2Variables(Map<String, Object> map) {
    setLaunchType("EC2");
    try {
      for (Entry<String, Object> entry : map.entrySet()) {
        if (!applyCommonVariable(entry.getKey(), entry.getValue())) {
          throw new InvalidRequestException(
              format("Unsupported infrastructure mapping provisioner variable %s", entry.getKey()));
        }
      }
    } catch (WingsException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new InvalidRequestException("Unable to set the provisioner variables to the mapping", exception);
    }
  }

  @Override
  public void applyProvisionerVariables(
      Map<String, Object> map, NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
    if (featureFlagEnabled) {
      applyProvisionerVariables(map);
    } else {
      switch (nodeFilteringType) {
        case AWS_ECS_EC2: {
          applyEcsEc2Variables(map);
          break;
        }
        case AWS_ECS_FARGATE: {
          applyEcsFargateVariables(map);
          break;
        }
        default: {
          // Should never happen
          throw new InvalidRequestException(format("Unidentified: [%s] node filtering type", nodeFilteringType.name()));
        }
      }
    }
    ensureSetString(getRegion(), "Region is required");
    ensureSetString(getClusterName(), "Cluster is required");
    if (AWS_ECS_FARGATE == nodeFilteringType
        || (featureFlagEnabled && LaunchType.FARGATE.toString().equals(getLaunchType()))) {
      ensureSetString(getExecutionRole(), "Task execution role is required for Fargate Launch type");
      ensureSetString(getVpcId(), "VpcId is required for Fargate Launch Type");
      ensureSetStringArray(getSecurityGroupIds(), "Security group ids are required for Fargate launch type");
      ensureSetStringArray(getSubnetIds(), "Subnet ids are required for Fargate launch type");
    }
  }

  @VisibleForTesting
  public void applyProvisionerVariables(Map<String, Object> resolvedBlueprints) {
    if (isNotEmpty(resolvedBlueprints)) {
      for (Map.Entry<String, Object> resolvedBlueprintEntry : resolvedBlueprints.entrySet()) {
        Object value = resolvedBlueprintEntry.getValue();
        switch (resolvedBlueprintEntry.getKey()) {
          case "region":
            setRegion((String) value);
            break;
          case "clusterName":
            setClusterName((String) value);
            break;
          case "vpcId":
            setVpcId((String) value);
            break;
          case "subnetIds":
            setSubnetIds(getList(value));
            break;
          case "securityGroupIds":
            setSecurityGroupIds(getList(value));
            break;
          case "executionRole":
            setExecutionRole((String) value);
            break;
          default:
            throw new InvalidRequestException(
                format("Unknown blueprint field : [%s]", resolvedBlueprintEntry.getKey()));
        }
      }
    }
  }

  private void ensureSetString(String field, String errorMessage) {
    if (isEmpty(field)) {
      throw new InvalidRequestException(errorMessage);
    }
  }

  private void ensureSetStringArray(List<String> fields, String errorMessage) {
    if (EmptyPredicate.isEmpty(fields)) {
      throw new InvalidRequestException(errorMessage);
    }
    if (fields.stream().anyMatch(EmptyPredicate::isEmpty)) {
      throw new InvalidRequestException(errorMessage);
    }
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Utils.normalize(format("%s (%s::%s) %s", isEmpty(this.getProvisionerId()) ? this.getClusterName() : "",
        this.getComputeProviderType(),
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
        this.getRegion()));
  }

  /**
   * Getter for property 'region'.
   *
   * @return Value for property 'region'.
   */
  public String getRegion() {
    return isNullOrEmpty(region) ? AWS_DEFAULT_REGION : region;
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
   * Getter for property 'vpcId'.
   *
   * @return Value for property 'vpcId'.
   */
  public String getVpcId() {
    return vpcId;
  }

  /**
   * Setter for property 'vpcId'.
   *
   * @param vpcId Value to set for property 'vpcId'.
   */
  public void setVpcId(String vpcId) {
    this.vpcId = vpcId;
  }

  /**
   * Getter for property 'subnetIds'.
   *
   * @return Value for property 'subnetIds'.
   */
  public List<String> getSubnetIds() {
    return subnetIds;
  }

  /**
   * Setter for property 'subnetIds'.
   *
   * @param subnetIds Value to set for property 'subnetIds'.
   */
  public void setSubnetIds(List<String> subnetIds) {
    this.subnetIds = subnetIds;
  }

  /**
   * Getter for property 'securityGroupIds'.
   *
   * @return Value for property 'securityGroupIds'.
   */
  public List<String> getSecurityGroupIds() {
    return securityGroupIds;
  }

  /**
   * Setter for property 'securityGroupIds'.
   *
   * @param securityGroupIds Value to set for property 'securityGroupIds'.
   */
  public void setSecurityGroupIds(List<String> securityGroupIds) {
    this.securityGroupIds = securityGroupIds;
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

  public boolean isAssignPublicIp() {
    return assignPublicIp;
  }

  public void setAssignPublicIp(boolean assignPublicIp) {
    this.assignPublicIp = assignPublicIp;
  }

  public String getExecutionRole() {
    return executionRole;
  }

  public void setExecutionRole(String executionRole) {
    this.executionRole = executionRole;
  }

  public String getLaunchType() {
    return launchType;
  }

  public void setLaunchType(String launchType) {
    this.launchType = launchType;
  }

  @Override
  public String getNamespace() {
    return null;
  }

  @Override
  public String getReleaseName() {
    return null;
  }

  @Override
  public void setReleaseName(String releaseName) {}

  public static final class Builder {
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    private String accountId;
    private String clusterName;
    private String region;
    private String vpcId;
    private List<String> subnetIds;
    private List<String> securityGroupIds;
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
    private boolean assignPublicIp;
    private String executionRole;
    private String launchType;

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

    public Builder withAssignPublicIp(boolean assignPublicIp) {
      this.assignPublicIp = assignPublicIp;
      return this;
    }

    public Builder withExecutionRole(String executionRole) {
      this.executionRole = executionRole;
      return this;
    }

    public Builder withLaunchType(String launchType) {
      this.launchType = launchType;
      return this;
    }

    public EcsInfrastructureMapping build() {
      EcsInfrastructureMapping ecsInfrastructureMapping = new EcsInfrastructureMapping();
      ecsInfrastructureMapping.setClusterName(clusterName);
      ecsInfrastructureMapping.setRegion(region);
      ecsInfrastructureMapping.setVpcId(vpcId);
      ecsInfrastructureMapping.setSubnetIds(subnetIds);
      ecsInfrastructureMapping.setSecurityGroupIds(securityGroupIds);
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
      ecsInfrastructureMapping.setAssignPublicIp(assignPublicIp);
      ecsInfrastructureMapping.setLaunchType(launchType);
      ecsInfrastructureMapping.setExecutionRole(executionRole);
      return ecsInfrastructureMapping;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ContainerInfrastructureMapping.YamlWithComputeProvider {
    private String region = AWS_DEFAULT_REGION;
    private String vpcId;
    private String subnetIds;
    private String securityGroupIds;
    private String launchType = LaunchType.EC2.name();
    private boolean assignPublicIp;
    private String executionRole;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String cluster, String region,
        String vpcId, String subnetIds, String securityGroupIds, String launchType, boolean assignPublicIp,
        String executionRole, Map<String, Object> blueprints) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName, cluster, blueprints);
      this.region = region;
      this.vpcId = vpcId;
      this.subnetIds = subnetIds;
      this.securityGroupIds = securityGroupIds;
      this.launchType = launchType;
      this.assignPublicIp = assignPublicIp;
      this.executionRole = executionRole;
    }
  }
}
