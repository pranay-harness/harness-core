package io.harness.batch.processing.tasklet.util;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.harness.logging.AutoLogContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

@Slf4j
public abstract class CacheUtils {
  public void logCacheStats() throws IllegalAccessException {
    for (Field field : this.getClass().getDeclaredFields()) {
      if (LoadingCache.class.equals(field.getType())) {
        field.setAccessible(true);
        LoadingCache loadingCache = (LoadingCache) field.get(this);
        try (AutoLogContext ignore1 = new LoadingCacheLogContext(field.getName(),
                 String.valueOf(loadingCache.estimatedSize()), AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
          logger.info(loadingCache.stats().toString());
        }
      }
    }
  }
}
