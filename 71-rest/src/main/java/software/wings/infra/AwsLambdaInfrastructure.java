package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

import java.util.List;

@JsonTypeName("AWS_AWS_LAMBDA")
@Data
public class AwsLambdaInfrastructure implements CloudProviderInfrastructure {
  private String cloudProviderId;
  private String region;
  private String vpcId;
  private List<String> subnetIds;
  private List<String> securityGroupIds;
  private String iamRole;
}
