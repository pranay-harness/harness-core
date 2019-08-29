package software.wings.infra;

import static java.lang.String.format;
import static software.wings.beans.InfrastructureType.AWS_LAMBDA;

import com.google.common.collect.ImmutableSet;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.exception.InvalidRequestException;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.StringUtils;
import software.wings.annotation.IncludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonTypeName("AWS_AWS_LAMBDA")
@Data
@Builder
@FieldNameConstants(innerTypeName = "AwsLambdaInfrastructureKeys")
public class AwsLambdaInfrastructure
    implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider, ProvisionerAware {
  private String cloudProviderId;
  @IncludeFieldMap private String region;
  private String vpcId;
  private List<String> subnetIds;
  private List<String> securityGroupIds;
  private String role;
  private Map<String, String> expressions;

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
  public void applyExpressions(
      Map<String, Object> resolvedExpressions, String appId, String envId, String infraDefinitionId) {
    for (Map.Entry<String, Object> entry : resolvedExpressions.entrySet()) {
      switch (entry.getKey()) {
        case "region":
          Validator.ensureType(String.class, entry.getValue(), "Region should be of String type");
          setRegion((String) entry.getValue());
          break;
        case "role":
          Validator.ensureType(String.class, entry.getValue(), "IAM Role should be of String type");
          setRole((String) entry.getValue());
          break;
        case "vpcId":
          Validator.ensureType(String.class, entry.getValue(), "Vpc Id should be of String type");
          setVpcId((String) entry.getValue());
          break;
        case "subnetIds":
          try {
            setSubnetIds(getList(entry.getValue()));
          } catch (ClassCastException e) {
            throw new InvalidRequestException(
                "Subnet Ids should be of List or comma-separated String type. Found : " + entry.getValue());
          }
          break;
        case "securityGroupIds":
          try {
            setSecurityGroupIds(getList(entry.getValue()));
          } catch (ClassCastException e) {
            throw new InvalidRequestException(
                "Security Groups should be of List or comma-separated String type. Found : " + entry.getValue());
          }
          break;
        default: { throw new InvalidRequestException(format("Unknown expression : [%s]", entry.getKey())); }
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
