package software.wings.beans.container;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AwsAutoScalarConfig {
  private String resourceId;
  private String scalableTargetJson;
  // This is Only For UI. UI will send single String, we will form
  // String[] scalingPolicyJson using it
  private String scalingPolicyForTarget;
  private String[] scalingPolicyJson;
}
