package io.harness.batch.processing.tasklet.util;

import io.harness.logging.AutoLogContext;

import com.google.common.collect.ImmutableMap;

public class LoadingCacheLogContext extends AutoLogContext {
  public static final String NAME = "cache_name";
  public static final String SIZE_COUNT = "size_count";

  public LoadingCacheLogContext(String cacheName, String sizeCount, OverrideBehavior behavior) {
    super(ImmutableMap.of(NAME, cacheName, SIZE_COUNT, sizeCount), behavior);
  }
}
