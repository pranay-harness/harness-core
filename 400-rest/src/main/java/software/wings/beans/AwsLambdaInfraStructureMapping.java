/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.exception.InvalidRequestException;

import software.wings.annotation.Blueprint;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.stencils.EnumData;
import software.wings.utils.Utils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.StringUtils;

/**
 * The type Aws lambda infra structure mapping.
 */
@JsonTypeName("AWS_AWS_LAMBDA")
@Data
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "AwsLambdaInfraStructureMappingKeys")
@OwnedBy(CDP)
@TargetModule(_957_CG_BEANS)
public class AwsLambdaInfraStructureMapping extends InfrastructureMapping {
  /**
   * Instantiates a new Infrastructure mapping.
   */
  public AwsLambdaInfraStructureMapping() {
    super(InfrastructureMappingType.AWS_AWS_LAMBDA.name());
  }

  @Attributes(title = "Region", required = true)
  @EnumData(enumDataProvider = AwsRegionDataProvider.class)
  @Blueprint
  private String region;

  @Blueprint @Attributes(title = "VPC") private String vpcId;
  @Blueprint @Attributes(title = "Subnets") private List<String> subnetIds = new ArrayList<>();
  @Blueprint @Attributes(title = "Security Groups") private List<String> securityGroupIds = new ArrayList<>();
  @Blueprint @Attributes(title = " IAM role") private String role;

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return null;
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Utils.normalize(format("%s (AWS_Lambda) %s",
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
        StringUtils.isEmpty(getProvisionerId()) ? this.getRegion() : ""));
  }

  @Override
  public void applyProvisionerVariables(
      Map<String, Object> map, NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      switch (entry.getKey()) {
        case "region":
          try {
            setRegion((String) entry.getValue());
          } catch (ClassCastException e) {
            throw new InvalidRequestException("Region should be of String type. Found : " + entry.getValue());
          }
          break;
        case "role":
          try {
            setRole((String) entry.getValue());
          } catch (ClassCastException e) {
            throw new InvalidRequestException("IAM Role should be of String type. Found : " + entry.getValue());
          }
          break;
        case "vpcId":
          try {
            setVpcId((String) entry.getValue());
          } catch (ClassCastException e) {
            throw new InvalidRequestException("Vpc Id should be of String type. Found : " + entry.getValue());
          }
          break;
        case "subnetIds":
          try {
            setSubnetIds(getList(entry.getValue()));
          } catch (ClassCastException e) {
            throw new InvalidRequestException(
                "Subnet Ids should be of List or comma-separated String type. Found : " + entry.getValue());
          }
          break;
        case "securityGroups":
        case "securityGroupIds":
          try {
            setSecurityGroupIds(getList(entry.getValue()));
          } catch (ClassCastException e) {
            throw new InvalidRequestException(
                "Security Groups should be of List or comma-separated String type. Found : " + entry.getValue());
          }
          break;
        default:
          throw new InvalidRequestException("UnSupported Provisioner Mapping " + entry.getKey());
      }
    }
    if (StringUtils.isEmpty(region)) {
      throw new InvalidRequestException("Region Mapping is Required");
    }
    if (StringUtils.isEmpty(role)) {
      throw new InvalidRequestException("Role Mapping is Required");
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends YamlWithComputeProvider {
    private String region;
    private String vpcId;
    private List<String> subnetIds = new ArrayList<>();
    private List<String> securityGroupIds = new ArrayList<>();
    private String role;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String region, String vpcId,
        List<String> subnetIds, List<String> securityGroupIds, String role, Map<String, Object> blueprints) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName, blueprints);
      this.region = region;
      this.vpcId = vpcId;
      this.subnetIds = subnetIds;
      this.securityGroupIds = securityGroupIds;
      this.role = role;
    }
  }

  @lombok.Builder
  public AwsLambdaInfraStructureMapping(String entityYamlPath, String appId, String accountId, String type, String uuid,
      EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt,
      String computeProviderSettingId, String envId, String serviceTemplateId, String serviceId,
      String computeProviderType, String infraMappingType, String deploymentType, String computeProviderName,
      String name, boolean autoPopulateName, Map<String, Object> blueprints, String provisionerId, String region,
      String vpcId, List<String> subnetIds, List<String> securityGroupIds, String role, boolean sample) {
    super(entityYamlPath, appId, accountId, type, uuid, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt,
        computeProviderSettingId, envId, serviceTemplateId, serviceId, computeProviderType, infraMappingType,
        deploymentType, computeProviderName, name, autoPopulateName, blueprints, provisionerId, sample);
    this.region = region;
    this.vpcId = vpcId;
    this.subnetIds = subnetIds;
    this.securityGroupIds = securityGroupIds;
    this.role = role;
  }
}
