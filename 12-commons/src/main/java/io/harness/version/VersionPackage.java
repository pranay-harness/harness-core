package io.harness.version;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VersionPackage {
  private VersionInfo version;
  private RuntimeInfo runtime;
}
