package io.harness.subscription;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.GTM)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionConfig {
  private String StripeApiKey;
  private int maxNetworkReties = 3;
  private int connectTimeout = 30000;
  private int readTimeout = 80000;
}
