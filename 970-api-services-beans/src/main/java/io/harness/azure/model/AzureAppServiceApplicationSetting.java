package io.harness.azure.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzureAppServiceApplicationSetting {
  private String name;
  private String value;
  private boolean sticky;
}
