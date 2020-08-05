package io.harness.delegate.task.azure.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMInstanceData {
  private String instanceId;
  private String publicDnsName;
  private String privateDnsName;
  private String privateIpAddress;
}
