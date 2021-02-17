package io.harness;

import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DecisionModuleConfiguration {
  private ServiceHttpClientConfig resourceGroupServiceConfig;
  private String resourceGroupServiceSecret;
}
