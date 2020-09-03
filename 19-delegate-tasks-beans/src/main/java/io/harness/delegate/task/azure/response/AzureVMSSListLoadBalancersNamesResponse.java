package io.harness.delegate.task.azure.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AzureVMSSListLoadBalancersNamesResponse implements AzureVMSSTaskResponse {
  private List<String> loadBalancersNames;
}
