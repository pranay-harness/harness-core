package software.wings.infra;

import static software.wings.beans.InfrastructureType.AWS_LAMBDA;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import java.util.List;

@JsonTypeName("AWS_AWS_LAMBDA")
@Data
@Builder
public class AwsLambdaInfrastructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  @ExcludeFieldMap private String cloudProviderId;
  private String region;
  private String vpcId;
  private List<String> subnetIds;
  private List<String> securityGroupIds;
  private String role;

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

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AWS;
  }

  public String getCloudProviderInfrastructureType() {
    return AWS_LAMBDA;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(AWS_LAMBDA)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
    private String cloudProviderName;
    private String region;
    private String vpcId;
    private List<String> subnetIds;
    private List<String> securityGroupIds;
    private String iamRole;

    @Builder
    public Yaml(String type, String cloudProviderName, String region, String vpcId, List<String> subnetIds,
        List<String> securityGroupIds, String iamRole) {
      super(type);
      setCloudProviderName(cloudProviderName);
      setRegion(region);
      setVpcId(vpcId);
      setSubnetIds(subnetIds);
      setSecurityGroupIds(securityGroupIds);
      setIamRole(iamRole);
    }

    public Yaml() {
      super(AWS_LAMBDA);
    }
  }
}
