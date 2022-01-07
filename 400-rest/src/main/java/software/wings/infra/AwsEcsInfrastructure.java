/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.infra;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.InfrastructureType.AWS_ECS;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;

import software.wings.annotation.IncludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.amazonaws.services.ecs.model.LaunchType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

@JsonTypeName("AWS_ECS")
@Data
@Builder
@FieldNameConstants(innerTypeName = "AwsEcsInfrastructureKeys")
@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class AwsEcsInfrastructure
    implements InfraMappingInfrastructureProvider, ContainerInfrastructure, FieldKeyValMapProvider, ProvisionerAware {
  private String cloudProviderId;
  @IncludeFieldMap private String region;
  private String vpcId;
  private List<String> subnetIds;
  private List<String> securityGroupIds;
  private boolean assignPublicIp;
  private String executionRole;
  // Do not allow to modify
  @IncludeFieldMap private String launchType;
  @IncludeFieldMap private String clusterName;
  @Getter(onMethod = @__(@JsonIgnore)) private String type;
  @Getter(onMethod = @__(@JsonIgnore)) private String role;
  @Getter(onMethod = @__(@JsonIgnore)) private int diskSize;
  @Getter(onMethod = @__(@JsonIgnore)) private String ami;
  @Getter(onMethod = @__(@JsonIgnore)) private int numberOfNodes;
  private Map<String, String> expressions;

  @Override
  public InfrastructureMapping getInfraMapping() {
    return anEcsInfrastructureMapping()
        .withComputeProviderSettingId(cloudProviderId)
        .withRegion(region)
        .withVpcId(vpcId)
        .withSubnetIds(subnetIds)
        .withSecurityGroupIds(securityGroupIds)
        .withAssignPublicIp(assignPublicIp)
        .withExecutionRole(executionRole)
        .withLaunchType(launchType)
        .withClusterName(clusterName)
        .withInfraMappingType(InfrastructureMappingType.AWS_ECS.name())
        .build();
  }

  @Override
  public Class<EcsInfrastructureMapping> getMappingClass() {
    return EcsInfrastructureMapping.class;
  }

  @Override
  public String getInfrastructureType() {
    return AWS_ECS;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AWS;
  }

  @Override
  public Set<String> getSupportedExpressions() {
    return ImmutableSet.of(AwsEcsInfrastructureKeys.region, AwsEcsInfrastructureKeys.vpcId,
        AwsEcsInfrastructureKeys.subnetIds, AwsEcsInfrastructureKeys.securityGroupIds,
        AwsEcsInfrastructureKeys.executionRole, AwsEcsInfrastructureKeys.clusterName);
  }

  @Override
  public void applyExpressions(
      Map<String, Object> resolvedExpressions, String appId, String envId, String infraDefinitionId) {
    if (isNotEmpty(resolvedExpressions)) {
      for (Map.Entry<String, Object> expression : resolvedExpressions.entrySet()) {
        Object value = expression.getValue();
        switch (expression.getKey()) {
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
            throw new InvalidRequestException(format("Unknown expression : [%s]", expression.getKey()));
        }
      }
    }

    ensureSetString(getRegion(), "Region is required");
    ensureSetString(getClusterName(), "Cluster is required");
    if (LaunchType.FARGATE.toString().equals(getLaunchType())) {
      ensureSetString(getExecutionRole(), "Task execution role is required for Fargate Launch type");
      ensureSetString(getVpcId(), "VpcId is required for Fargate Launch Type");
      ensureSetStringArray(getSecurityGroupIds(), "Security group ids are required for Fargate launch type");
      ensureSetStringArray(getSubnetIds(), "Subnet ids are required for Fargate launch type");
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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(AWS_ECS)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
    private String cloudProviderName;
    private String region;
    private String vpcId;
    private List<String> subnetIds;
    private List<String> securityGroupIds;
    private boolean assignPublicIp;
    private String executionRole;
    private String launchType;
    private Map<String, String> expressions;
    private String clusterName;

    @Builder
    public Yaml(String type, String cloudProviderName, String region, String vpcId, List<String> subnetIds,
        List<String> securityGroupIds, boolean assignPublicIp, String executionRole, String launchType,
        Map<String, String> expressions, String clusterName) {
      super(type);
      setCloudProviderName(cloudProviderName);
      setRegion(region);
      setVpcId(vpcId);
      setSubnetIds(subnetIds);
      setSecurityGroupIds(securityGroupIds);
      setAssignPublicIp(assignPublicIp);
      setExecutionRole(executionRole);
      setLaunchType(launchType);
      setExpressions(expressions);
      setClusterName(clusterName);
    }

    public Yaml() {
      super(AWS_ECS);
    }
  }
}
