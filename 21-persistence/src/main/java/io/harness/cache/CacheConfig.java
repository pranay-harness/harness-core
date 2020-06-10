package io.harness.cache;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class CacheConfig {
  CacheBackend cacheBackend;
  String cacheNamespace;
  Set<String> disabledCaches;
}
