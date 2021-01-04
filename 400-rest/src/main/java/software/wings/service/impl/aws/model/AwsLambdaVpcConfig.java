package software.wings.service.impl.aws.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsLambdaVpcConfig {
  private String vpcId;
  private List<String> subnetIds;
  private List<String> securityGroupIds;
}
