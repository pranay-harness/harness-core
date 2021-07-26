package io.harness.accesscontrol.principals.serviceaccounts;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class ServiceAccountClientConfiguration {
  private ServiceHttpClientConfig serviceAccountServiceConfig;
  private String serviceAccountServiceSecret;
}
