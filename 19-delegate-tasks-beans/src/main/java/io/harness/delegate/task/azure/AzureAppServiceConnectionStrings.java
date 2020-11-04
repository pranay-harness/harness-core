package io.harness.delegate.task.azure;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzureAppServiceConnectionStrings {
  private String connectionSettingName;
  private String connectionSettingValue;
  private String connectionSettingType;
  private boolean deploymentSlotSetting;
}
