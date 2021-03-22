package io.harness.accesscontrol.migrations;

import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectOrgsClientConfiguration {
  private ServiceHttpClientConfig projectOrgsServiceConfig;
  private String projectOrgsServiceSecret;
}
