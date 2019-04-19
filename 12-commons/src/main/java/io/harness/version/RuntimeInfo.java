package io.harness.version;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RuntimeInfo {
  private boolean primary;
  private String primaryVersion;
}
