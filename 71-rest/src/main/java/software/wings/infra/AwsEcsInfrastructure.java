package software.wings.infra;

import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.InfrastructureType.AWS_ECS;

import com.google.common.collect.ImmutableSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonTypeName("AWS_ECS")
@Data
@Builder
@FieldNameConstants(innerTypeName = "AwsEcsInfrastructureKeys")
public class AwsEcsInfrastructure
    implements InfraMappingInfrastructureProvider, ContainerInfrastructure, FieldKeyValMapProvider, ProvisionerAware {
  @ExcludeFieldMap private String cloudProviderId;
  private String region;
  private String vpcId;
  private List<String> subnetIds;
  private List<String> securityGroupIds;
  private boolean assignPublicIp;
  private String executionRole;
  private String launchType;
  private String clusterName;
  @Getter(onMethod = @__(@JsonIgnore)) private String type;
  @Getter(onMethod = @__(@JsonIgnore)) private String role;
  @Getter(onMethod = @__(@JsonIgnore)) private int diskSize;
  @Getter(onMethod = @__(@JsonIgnore)) private String ami;
  @Getter(onMethod = @__(@JsonIgnore)) private int numberOfNodes;
  @ExcludeFieldMap private Map<String, String> expressions;

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
  public void applyExpressions(Map<String, Object> resolvedExpressions) {}

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

    @Builder
    public Yaml(String type, String cloudProviderName, String region, String vpcId, List<String> subnetIds,
        List<String> securityGroupIds, boolean assignPublicIp, String executionRole, String launchType,
        Map<String, String> expressions) {
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
    }

    public Yaml() {
      super(AWS_ECS);
    }
  }
}
