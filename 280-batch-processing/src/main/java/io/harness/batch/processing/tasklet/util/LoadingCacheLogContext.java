package io.harness.batch.processing.tasklet.util;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AutoLogContext;

public class LoadingCacheLogContext extends AutoLogContext {
  public static final String NAME = "cache_name";
  public static final String SIZE_COUNT = "size_count";

  public LoadingCacheLogContext(String cacheName, String sizeCount, OverrideBehavior behavior) {
    super(ImmutableMap.of(NAME, cacheName, SIZE_COUNT, sizeCount), behavior);
  }
}
