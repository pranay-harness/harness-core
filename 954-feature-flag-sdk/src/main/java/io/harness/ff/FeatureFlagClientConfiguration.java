package io.harness.ff;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
public class FeatureFlagClientConfiguration {
  private ServiceHttpClientConfig featureFlagServiceConfig;
  private String featureFlagServiceSecret;
}
