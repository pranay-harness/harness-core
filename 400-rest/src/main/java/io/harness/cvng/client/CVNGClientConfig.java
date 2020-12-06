package io.harness.cvng.client;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CVNGClientConfig {
  private String baseUrl;
  private String cvNgServiceSecret;
}
