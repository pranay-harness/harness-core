package software.wings.infra;

import static software.wings.beans.InfrastructureType.AWS_LAMBDA;

import com.google.common.collect.ImmutableSet;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonTypeName("AWS_AWS_LAMBDA")
@Data
@Builder
@FieldNameConstants(innerTypeName = "AwsLambdaInfrastructureKeys")
public class AwsLambdaInfrastructure
    implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider, ProvisionerAware {
  @ExcludeFieldMap private String cloudProviderId;
  private String region;
  private String vpcId;
  private List<String> subnetIds;
  private List<String> securityGroupIds;
  private String role;
  @ExcludeFieldMap private Map<String, String> expressions;

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

  public String getInfrastructureType() {
    return AWS_LAMBDA;
  }

  @Override
  public Set<String> getSupportedExpressions() {
    return ImmutableSet.of(AwsLambdaInfrastructureKeys.region, AwsLambdaInfrastructureKeys.subnetIds,
        AwsLambdaInfrastructureKeys.securityGroupIds, AwsLambdaInfrastructureKeys.vpcId,
        AwsLambdaInfrastructureKeys.role);
  }

  @Override
  public void applyExpressions(Map<String, Object> resolvedExpressions) {}

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
    private Map<String, String> expressions;

    @Builder
    public Yaml(String type, String cloudProviderName, String region, String vpcId, List<String> subnetIds,
        List<String> securityGroupIds, String iamRole, Map<String, String> expressions) {
      super(type);
      setCloudProviderName(cloudProviderName);
      setRegion(region);
      setVpcId(vpcId);
      setSubnetIds(subnetIds);
      setSecurityGroupIds(securityGroupIds);
      setIamRole(iamRole);
      setExpressions(expressions);
    }

    public Yaml() {
      super(AWS_LAMBDA);
    }
  }
}
