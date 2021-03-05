package io.harness;

import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccessControlClientConfiguration {
  private boolean enableAccessControl;
  private ServiceHttpClientConfig accessControlServiceConfig;
  private String accessControlServiceSecret;
}
