package io.harness.delegate.task.azure;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzureAppServiceApplicationSettings {
  private String settingName;
  private String settingValue;
  private boolean deploymentSlotSetting;
}
