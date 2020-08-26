package io.harness.delegate.task.azure.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureLoadBalancerDetailForBGDeployment {
  private String loadBalancerName;
  private String prodListenerPort;
  private String prodBackendPool;
  private String stageListenerPort;
  private String stageBackendPool;
}
