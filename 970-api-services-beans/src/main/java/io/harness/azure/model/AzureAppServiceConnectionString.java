package io.harness.azure.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzureAppServiceConnectionString {
  private String name;
  private String value;
  private AzureAppServiceConnectionStringType type;
  private boolean sticky;
}
