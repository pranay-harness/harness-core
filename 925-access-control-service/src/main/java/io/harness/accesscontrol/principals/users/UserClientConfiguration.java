package io.harness.accesscontrol.principals.users;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
public class UserClientConfiguration {
  private ServiceHttpClientConfig userServiceConfig;
  private String userServiceSecret;
}
