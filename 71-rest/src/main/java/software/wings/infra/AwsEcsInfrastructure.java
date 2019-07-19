package software.wings.infra;

import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import software.wings.annotation.IncludeInFieldMap;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping.EcsInfrastructureMappingKeys;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

import java.util.List;

@JsonTypeName("AWS_ECS")
@Data
public class AwsEcsInfrastructure implements InfraMappingInfrastructureProvider {
  private String cloudProviderId;

  @IncludeInFieldMap(key = EcsInfrastructureMappingKeys.region) private String region;

  @IncludeInFieldMap(key = EcsInfrastructureMappingKeys.vpcId) private String vpcId;

  @IncludeInFieldMap(key = EcsInfrastructureMappingKeys.subnetIds) private List<String> subnetIds;

  @IncludeInFieldMap(key = EcsInfrastructureMappingKeys.securityGroupIds) private List<String> securityGroupIds;

  @IncludeInFieldMap(key = EcsInfrastructureMappingKeys.assignPublicIp) private boolean assignPublicIp;

  @IncludeInFieldMap(key = EcsInfrastructureMappingKeys.executionRole) private String executionRole;

  @IncludeInFieldMap(key = EcsInfrastructureMappingKeys.launchType) private String launchType;

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
}
