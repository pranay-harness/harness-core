package io.harness.cvng.client;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(PL)
public class CVNGClientConfig {
  private String baseUrl;
  private String cvNgServiceSecret;
}
