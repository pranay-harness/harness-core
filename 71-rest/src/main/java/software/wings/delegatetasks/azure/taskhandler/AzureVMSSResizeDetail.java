package software.wings.delegatetasks.azure.taskhandler;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSResizeDetail {
  private String scaleSetName;
  private int desiredCount;
  private List<String> scalingPolicyJSONs;
  private boolean attachScalingPolicy;
  private String scalingPolicyMessage;
}
