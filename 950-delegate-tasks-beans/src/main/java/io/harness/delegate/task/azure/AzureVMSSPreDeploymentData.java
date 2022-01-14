package io.harness.delegate.task.azure;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSPreDeploymentData {
  private String oldVmssName;
  private int minCapacity;
  private int desiredCapacity;
  private List<String> scalingPolicyJSON;
}
