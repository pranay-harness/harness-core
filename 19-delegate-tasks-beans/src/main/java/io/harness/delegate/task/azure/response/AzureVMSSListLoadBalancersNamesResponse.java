package io.harness.delegate.task.azure.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSListLoadBalancersNamesResponse implements AzureVMSSTaskResponse {
  private List<String> loadBalancersNames;
}
