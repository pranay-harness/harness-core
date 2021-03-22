package software.wings.beans.container;

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
public class AwsAutoScalarConfig {
  private String resourceId;
  private String scalableTargetJson;
  // This is Only For UI. UI will send single String, we will form
  // String[] scalingPolicyJson using it
  private String scalingPolicyForTarget;
  private String[] scalingPolicyJson;
}
