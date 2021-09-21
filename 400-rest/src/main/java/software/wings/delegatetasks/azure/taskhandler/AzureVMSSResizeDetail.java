package software.wings.delegatetasks.azure.taskhandler;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(HarnessTeam.DEL)
public class AzureVMSSResizeDetail {
  private String scaleSetName;
  private int desiredCount;
  private List<String> scalingPolicyJSONs;
  private boolean attachScalingPolicy;
  private String scalingPolicyMessage;
}
