package io.harness.delegate.task.azure.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AzureVMSSListLoadBalancerBackendPoolsNamesResponse implements AzureVMSSTaskResponse {
  private List<String> loadBalancerBackendPoolsNames;
}
