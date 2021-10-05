package io.harness.perpetualtask.k8s.utils;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.time.Duration;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
@OwnedBy(HarnessTeam.CE)
public class ApiExceptionLogger {
  private final Cache<String, Boolean> recentlyLoggedExceptions =
      Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).build();

  public void logErrorIfNotSeenRecently(Exception ex, String msg) {
    recentlyLoggedExceptions.get(msg, k -> {
      log.error(msg, ex);
      return Boolean.TRUE;
    });
  }

  public void logWarnIfNotSeenRecently(Exception ex, String msg) {
    recentlyLoggedExceptions.get(msg, k -> {
      log.warn(msg, ex);
      return Boolean.TRUE;
    });
  }

  public CacheStats stats() {
    return recentlyLoggedExceptions.stats();
  }
}
