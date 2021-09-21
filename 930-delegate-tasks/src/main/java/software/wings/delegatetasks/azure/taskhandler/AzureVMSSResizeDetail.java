package software.wings.delegatetasks.azure.taskhandler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class AzureVMSSResizeDetail {
  private String scaleSetName;
  private int desiredCount;
  private List<String> scalingPolicyJSONs;
  private boolean attachScalingPolicy;
  private String scalingPolicyMessage;
}
