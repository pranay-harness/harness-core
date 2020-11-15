package io.harness.azure.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSAutoScaleSettingsData {
  private int minInstances;
  private int maxInstances;
  private int desiredInstances;
}
