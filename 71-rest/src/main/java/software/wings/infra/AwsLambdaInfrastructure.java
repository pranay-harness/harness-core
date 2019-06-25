package software.wings.infra;

import lombok.Data;

import java.util.List;

@Data
public class AwsLambdaInfrastructure implements CloudProviderInfrastructure {
  private String cloudProviderId;
  private String region;
  private String vpcId;
  private List<String> subnetIds;
  private List<String> securityGroupIds;
  private String iamRole;
}
