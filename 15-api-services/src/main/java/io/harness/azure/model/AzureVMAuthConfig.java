package io.harness.azure.model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString(exclude = "key")
public class AzureVMAuthConfig {
  char[] key;
}
