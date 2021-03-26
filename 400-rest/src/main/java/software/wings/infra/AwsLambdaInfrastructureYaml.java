package software.wings.infra;

import static software.wings.beans.InfrastructureType.AWS_LAMBDA;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(AWS_LAMBDA)
public final class AwsLambdaInfrastructureYaml extends CloudProviderInfrastructureYaml {
  private String cloudProviderName;
  private String region;
  private String vpcId;
  private List<String> subnetIds;
  private List<String> securityGroupIds;
  private String iamRole;
  private Map<String, String> expressions;

  @Builder
  public AwsLambdaInfrastructureYaml(String type, String cloudProviderName, String region, String vpcId,
      List<String> subnetIds, List<String> securityGroupIds, String iamRole, Map<String, String> expressions) {
    super(type);
    setCloudProviderName(cloudProviderName);
    setRegion(region);
    setVpcId(vpcId);
    setSubnetIds(subnetIds);
    setSecurityGroupIds(securityGroupIds);
    setIamRole(iamRole);
    setExpressions(expressions);
  }

  public AwsLambdaInfrastructureYaml() {
    super(AWS_LAMBDA);
  }
}
