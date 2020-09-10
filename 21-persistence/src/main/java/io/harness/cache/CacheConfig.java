package io.harness.cache;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@OwnedBy(PL)
@Value
@Builder
public class CacheConfig {
  CacheBackend cacheBackend;
  String cacheNamespace;
  Set<String> disabledCaches;
}
