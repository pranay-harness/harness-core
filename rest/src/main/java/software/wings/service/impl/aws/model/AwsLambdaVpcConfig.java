package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AwsLambdaVpcConfig {
  private String vpcId;
  private List<String> subnetIds;
  private List<String> securityGroupIds;
}