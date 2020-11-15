package io.harness.azure.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMData {
  private String id;
  private String ip;
  private String publicDns;
  private String powerState;
  private String size;
}
