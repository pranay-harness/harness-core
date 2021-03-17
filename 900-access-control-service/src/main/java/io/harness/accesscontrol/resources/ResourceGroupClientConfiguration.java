package io.harness.accesscontrol.resources;

import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceGroupClientConfiguration {
  private ServiceHttpClientConfig resourceGroupServiceConfig;
  private String resourceGroupServiceSecret;
}
