/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.beans.AmiDeploymentType.AWS_ASG;
import static software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType.AWS_ASG_AMI;
import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;

import software.wings.annotation.Blueprint;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.utils.Utils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by anubhaw on 12/19/17.
 */
@JsonTypeName("AWS_AMI")
@FieldNameConstants(innerTypeName = "AwsAmiInfrastructureMappingKeys")
@OwnedBy(CDP)
@TargetModule(_957_CG_BEANS)
public class AwsAmiInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Region")
  @DefaultValue(AWS_DEFAULT_REGION)
  @EnumData(enumDataProvider = AwsRegionDataProvider.class)
  @Blueprint
  private String region;

  @Blueprint private String autoScalingGroupName;
  @Blueprint private List<String> classicLoadBalancers;
  @Blueprint private List<String> targetGroupArns;
  private String hostNameConvention;

  // Variables for B/G type Ami deployment
  @Blueprint private List<String> stageClassicLoadBalancers;
  @Blueprint private List<String> stageTargetGroupArns;

  // Right now ONLY regular Asg OR SpotInst
  private AmiDeploymentType amiDeploymentType = AWS_ASG;

  // Variables used for SpotInst Deployment type
  private String spotinstElastiGroupJson;
  private String spotinstCloudProvider;

  public AwsAmiInfrastructureMapping() {
    super(InfrastructureMappingType.AWS_AMI.name());
  }

  @Override
  public void applyProvisionerVariables(
      Map<String, Object> map, NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
    if (AWS_ASG != getAmiDeploymentType()) {
      // Should never happen
      throw new InvalidRequestException("Provisioning ONLY supported for AWS_ASG type AMI deployments");
    }

    if (!featureFlagEnabled && AWS_ASG_AMI != nodeFilteringType) {
      // Should never happen
      throw new InvalidRequestException(format("Unidentified: [%s] node filtering type", nodeFilteringType.name()));
    }

    // Clear the existing values
    setRegion(StringUtils.EMPTY);
    setAutoScalingGroupName(StringUtils.EMPTY);
    setClassicLoadBalancers(emptyList());
    setTargetGroupArns(emptyList());
    setStageClassicLoadBalancers(emptyList());
    setStageTargetGroupArns(emptyList());

    for (Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      switch (key) {
        case "region":
          setRegion((String) value);
          break;
        case "baseAsg":
        case "autoScalingGroupName":
          setAutoScalingGroupName((String) value);
          break;
        case "classicLoadBalancers":
        case "classicLbs":
          setClassicLoadBalancers(getList(value));
          break;
        case "targetGroups":
        case "targetGroupArns":
          setTargetGroupArns(getList(value));
          break;
        case "stageClassicLbs":
        case "stageClassicLoadBalancers":
          setStageClassicLoadBalancers(getList(value));
          break;
        case "stageTargetGroups":
        case "stageTargetGroupArns":
          setStageTargetGroupArns(getList(value));
          break;
        default: {
          throw new InvalidRequestException(
              format("Unidentified: [%s] key in properties map for Ami Asg deployment", key));
        }
      }
    }
    if (EmptyPredicate.isEmpty(getRegion())) {
      throw new InvalidRequestException("Region is required");
    }
    if (EmptyPredicate.isEmpty(getAutoScalingGroupName())) {
      throw new InvalidRequestException("Base Asg is required");
    }
  }

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return null;
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Utils.normalize(format("%s (AWS_AMI) %s",
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
        this.getRegion()));
  }
  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getAutoScalingGroupName() {
    return autoScalingGroupName;
  }

  public void setAutoScalingGroupName(String autoScalingGroupName) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  public List<String> getClassicLoadBalancers() {
    return classicLoadBalancers;
  }

  public void setClassicLoadBalancers(List<String> classicLoadBalancers) {
    this.classicLoadBalancers = classicLoadBalancers;
  }

  public List<String> getTargetGroupArns() {
    return targetGroupArns;
  }

  public void setTargetGroupArns(List<String> targetGroupArns) {
    this.targetGroupArns = targetGroupArns;
  }

  public String getHostNameConvention() {
    return hostNameConvention;
  }

  public void setHostNameConvention(String hostNameConvention) {
    this.hostNameConvention = hostNameConvention;
  }

  public List<String> getStageClassicLoadBalancers() {
    return stageClassicLoadBalancers;
  }

  public void setStageClassicLoadBalancers(List<String> stageClassicLoadBalancers) {
    this.stageClassicLoadBalancers = stageClassicLoadBalancers;
  }

  public List<String> getStageTargetGroupArns() {
    return stageTargetGroupArns;
  }

  public void setStageTargetGroupArns(List<String> stageTargetGroupArns) {
    this.stageTargetGroupArns = stageTargetGroupArns;
  }

  public AmiDeploymentType getAmiDeploymentType() {
    // Default to AWS_ASG
    return (amiDeploymentType != null) ? amiDeploymentType : AWS_ASG;
  }

  public void setAmiDeploymentType(AmiDeploymentType amiDeploymentType) {
    this.amiDeploymentType = amiDeploymentType;
  }

  public String getSpotinstElastiGroupJson() {
    return spotinstElastiGroupJson;
  }

  public void setSpotinstElastiGroupJson(String spotinstElastiGroupJson) {
    this.spotinstElastiGroupJson = spotinstElastiGroupJson;
  }

  public String getSpotinstCloudProvider() {
    return spotinstCloudProvider;
  }

  public void setSpotinstCloudProvider(String spotinstCloudProvider) {
    this.spotinstCloudProvider = spotinstCloudProvider;
  }

  public static final class Builder {
    private AwsAmiInfrastructureMapping awsAmiInfrastructureMapping;

    private Builder() {
      awsAmiInfrastructureMapping = new AwsAmiInfrastructureMapping();
    }

    public static Builder anAwsAmiInfrastructureMapping() {
      return new Builder();
    }

    public Builder withRegion(String region) {
      awsAmiInfrastructureMapping.setRegion(region);
      return this;
    }

    public Builder withAutoScalingGroupName(String autoScalingGroupName) {
      awsAmiInfrastructureMapping.setAutoScalingGroupName(autoScalingGroupName);
      return this;
    }

    public Builder withClassicLoadBalancers(List<String> classicLoadBalancers) {
      awsAmiInfrastructureMapping.setClassicLoadBalancers(classicLoadBalancers);
      return this;
    }

    public Builder withTargetGroupArns(List<String> targetGroupArns) {
      awsAmiInfrastructureMapping.setTargetGroupArns(targetGroupArns);
      return this;
    }

    public Builder withStageClassicLoadBalancers(List<String> stageClassicLoadBalancers) {
      awsAmiInfrastructureMapping.setStageClassicLoadBalancers(stageClassicLoadBalancers);
      return this;
    }

    public Builder withStageTargetGroupArns(List<String> stageTargetGroupArns) {
      awsAmiInfrastructureMapping.setStageTargetGroupArns(stageTargetGroupArns);
      return this;
    }

    public Builder withAmiDeploymentType(AmiDeploymentType amiDeploymentType) {
      awsAmiInfrastructureMapping.setAmiDeploymentType(amiDeploymentType);
      return this;
    }

    public Builder withSpotinstElastiGroupJson(String spotinstElastiGroupJson) {
      awsAmiInfrastructureMapping.setSpotinstElastiGroupJson(spotinstElastiGroupJson);
      return this;
    }

    public Builder withSpotinstCloudProvider(String spotinstCloudProvider) {
      awsAmiInfrastructureMapping.setSpotinstCloudProvider(spotinstCloudProvider);
      return this;
    }

    public Builder withUuid(String uuid) {
      awsAmiInfrastructureMapping.setUuid(uuid);
      return this;
    }

    public Builder withAppId(String appId) {
      awsAmiInfrastructureMapping.setAppId(appId);
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      awsAmiInfrastructureMapping.setCreatedBy(createdBy);
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      awsAmiInfrastructureMapping.setCreatedAt(createdAt);
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      awsAmiInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      awsAmiInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      return this;
    }

    public Builder withComputeProviderSettingId(String computeProviderSettingId) {
      awsAmiInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      return this;
    }

    public Builder withEnvId(String envId) {
      awsAmiInfrastructureMapping.setEnvId(envId);
      return this;
    }

    public Builder withServiceTemplateId(String serviceTemplateId) {
      awsAmiInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      return this;
    }

    public Builder withServiceId(String serviceId) {
      awsAmiInfrastructureMapping.setServiceId(serviceId);
      return this;
    }

    public Builder withComputeProviderType(String computeProviderType) {
      awsAmiInfrastructureMapping.setComputeProviderType(computeProviderType);
      return this;
    }

    public Builder withInfraMappingType(String infraMappingType) {
      awsAmiInfrastructureMapping.setInfraMappingType(infraMappingType);
      return this;
    }

    public Builder withDeploymentType(String deploymentType) {
      awsAmiInfrastructureMapping.setDeploymentType(deploymentType);
      return this;
    }

    public Builder withComputeProviderName(String computeProviderName) {
      awsAmiInfrastructureMapping.setComputeProviderName(computeProviderName);
      return this;
    }

    public Builder withName(String name) {
      awsAmiInfrastructureMapping.setName(name);
      return this;
    }

    public Builder withAccountId(String accountId) {
      awsAmiInfrastructureMapping.setAccountId(accountId);
      return this;
    }

    public Builder withHostNameConvention(String hostNameConvention) {
      awsAmiInfrastructureMapping.setHostNameConvention(hostNameConvention);
      return this;
    }

    public AwsAmiInfrastructureMapping build() {
      return awsAmiInfrastructureMapping;
    }
  }
}
