package io.harness.delegate.task.azure.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzureLoadBalancerDetailForBGDeployment {
  private String loadBalancerName;
  private String prodBackendPool;
  private String stageBackendPool;
}
