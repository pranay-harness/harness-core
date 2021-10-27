package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ConfigSecret;
import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PL)
public class AccessControlClientConfiguration {
  private boolean enableAccessControl;
  private ServiceHttpClientConfig accessControlServiceConfig;
  @ConfigSecret private String accessControlServiceSecret;
}
