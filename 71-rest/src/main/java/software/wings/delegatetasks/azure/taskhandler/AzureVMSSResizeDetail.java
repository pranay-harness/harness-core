package software.wings.delegatetasks.azure.taskhandler;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AzureVMSSResizeDetail {
  private String scaleSetName;
  private int desiredCount;
  private List<String> scalingPolicyJSONs;
  private boolean attachScalingPolicy;
}
