package io.harness.accesscontrol.scopes.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
public class AccountClientConfiguration {
  ServiceHttpClientConfig accountServiceConfig;
  String accountServiceSecret;
}
