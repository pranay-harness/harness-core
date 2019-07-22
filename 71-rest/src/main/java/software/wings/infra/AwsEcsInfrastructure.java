package software.wings.infra;

import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

import java.util.List;

@JsonTypeName("AWS_ECS")
@Data
public class AwsEcsInfrastructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  @ExcludeFieldMap private String cloudProviderId;

  private String region;

  private String vpcId;

  private List<String> subnetIds;

  private List<String> securityGroupIds;

  private boolean assignPublicIp;

  private String executionRole;

  private String launchType;

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
        .withInfraMappingType(InfrastructureMappingType.AWS_ECS.name())
        .build();
  }

  @Override
  public Class<EcsInfrastructureMapping> getMappingClass() {
    return EcsInfrastructureMapping.class;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AWS;
  }
}
