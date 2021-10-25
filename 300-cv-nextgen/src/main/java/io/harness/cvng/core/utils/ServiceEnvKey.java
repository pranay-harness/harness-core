package io.harness.cvng.core.utils;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceEnvKey {
  private String serviceIdentifier;
  private String envIdentifier;
}
