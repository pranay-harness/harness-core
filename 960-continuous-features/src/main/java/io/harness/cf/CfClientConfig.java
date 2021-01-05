package io.harness.cf;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CfClientConfig {
  private String apiKey;
}