package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OwnedBy(CDP)
public class AwsElbConfig {
  private String loadBalancerName;
  private String targetGroupArn;
  private String targetPort;
  private String targetContainerName;
  private String roleArn;
}
