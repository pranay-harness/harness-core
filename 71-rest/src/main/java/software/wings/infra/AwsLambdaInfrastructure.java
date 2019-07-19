package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.annotation.IncludeInFieldMap;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

import java.util.List;

@JsonTypeName("AWS_AWS_LAMBDA")
@Data
public class AwsLambdaInfrastructure implements InfraMappingInfrastructureProvider {
  private String cloudProviderId;
  @IncludeInFieldMap private String region;
  @IncludeInFieldMap private String vpcId;
  @IncludeInFieldMap private List<String> subnetIds;
  @IncludeInFieldMap private List<String> securityGroupIds;
  @IncludeInFieldMap private String role;

  @Override
  public InfrastructureMapping getInfraMapping() {
    return AwsLambdaInfraStructureMapping.builder()
        .computeProviderSettingId(cloudProviderId)
        .region(region)
        .vpcId(vpcId)
        .subnetIds(subnetIds)
        .securityGroupIds(securityGroupIds)
        .role(role)
        .infraMappingType(InfrastructureMappingType.AWS_AWS_LAMBDA.name())
        .build();
  }

  @Override
  public Class<AwsLambdaInfraStructureMapping> getMappingClass() {
    return AwsLambdaInfraStructureMapping.class;
  }
}
