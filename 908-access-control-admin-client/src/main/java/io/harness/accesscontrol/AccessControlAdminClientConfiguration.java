package io.harness.accesscontrol;

import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccessControlAdminClientConfiguration {
  ServiceHttpClientConfig accessControlServiceConfig;
  String accessControlServiceSecret;
}
