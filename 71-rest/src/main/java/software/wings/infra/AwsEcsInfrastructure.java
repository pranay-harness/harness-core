package software.wings.infra;

import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.InfrastructureType.AWS_ECS;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import java.util.List;

@JsonTypeName("AWS_ECS")
@Data
@Builder
public class AwsEcsInfrastructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  @ExcludeFieldMap private String cloudProviderId;
  private String region;
  private String vpcId;
  private List<String> subnetIds;
  private List<String> securityGroupIds;
  private boolean assignPublicIp;
  private String executionRole;
  private String launchType;
  private String clusterName;
  @SchemaIgnore private String type;
  @SchemaIgnore private String role;
  @SchemaIgnore private int diskSize;
  @SchemaIgnore private String ami;
  @SchemaIgnore private int numberOfNodes;

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

  public String getCloudProviderInfrastructureType() {
    return AWS_ECS;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AWS;
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

    @Builder
    public Yaml(String type, String cloudProviderName, String region, String vpcId, List<String> subnetIds,
        List<String> securityGroupIds, boolean assignPublicIp, String executionRole, String launchType) {
      super(type);
      setCloudProviderName(cloudProviderName);
      setRegion(region);
      setVpcId(vpcId);
      setSubnetIds(subnetIds);
      setSecurityGroupIds(securityGroupIds);
      setAssignPublicIp(assignPublicIp);
      setExecutionRole(executionRole);
      setLaunchType(launchType);
    }

    public Yaml() {
      super(AWS_ECS);
    }
  }
}
