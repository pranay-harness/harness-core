package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.secret.ConfigSecret;

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
