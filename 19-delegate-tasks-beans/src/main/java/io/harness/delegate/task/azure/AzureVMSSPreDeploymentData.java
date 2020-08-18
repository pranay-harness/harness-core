package io.harness.delegate.task.azure;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AzureVMSSPreDeploymentData {
  private String oldVmssName;
  private int minCapacity;
  private int desiredCapacity;
  private List<String> scalingPolicyJSON;
}
