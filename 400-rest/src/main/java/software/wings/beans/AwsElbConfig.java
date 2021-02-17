package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwsElbConfig {
  private String loadBalancerName;
  private String targetGroupArn;
  private String targetPort;
  private String targetContainerName;
  private String roleArn;
}
