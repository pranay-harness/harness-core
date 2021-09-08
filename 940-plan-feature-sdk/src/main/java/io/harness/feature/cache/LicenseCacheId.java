package io.harness.feature.cache;

import io.harness.ModuleType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LicenseCacheId {
  private String accountIdentifier;
  private ModuleType moduleType;
}
