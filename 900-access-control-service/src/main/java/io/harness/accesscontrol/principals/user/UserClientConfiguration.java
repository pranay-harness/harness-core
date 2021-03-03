package io.harness.accesscontrol.principals.user;

import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserClientConfiguration {
  private ServiceHttpClientConfig userServiceConfig;
  private String userServiceSecret;
}
