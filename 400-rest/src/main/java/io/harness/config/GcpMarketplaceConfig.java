package io.harness.config;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@Value
@Builder
public class GcpMarketplaceConfig {
  boolean enabled;
  String subscriptionName;
}
