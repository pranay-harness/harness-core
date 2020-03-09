package io.harness.batch.processing.billing.writer.support;

import com.google.inject.Inject;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.harness.ccm.cluster.entities.LastReceivedPublishedMessage;
import io.harness.ccm.health.LastReceivedPublishedMessageDao;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Get decide if need to generate billing data or not.
 */
@Component
@Slf4j
public class BillingDataGenerationValidator {
  private final LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;

  LoadingCache<BillingDataGenerationValidator.CacheKey, Boolean> billingDataGenerationValidationCache =
      Caffeine.newBuilder()
          .expireAfterWrite(6, TimeUnit.HOURS)
          .build(key -> fetchClusterBillingDataValidationInfo(key.accountId, key.clusterId, key.startTime));

  @Value
  private static class CacheKey {
    private String accountId;
    private String clusterId;
    private Instant startTime;
  }

  @Inject
  public BillingDataGenerationValidator(LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao) {
    this.lastReceivedPublishedMessageDao = lastReceivedPublishedMessageDao;
  }

  public boolean shouldGenerateBillingData(String accountId, String clusterId, Instant startTime) {
    final BillingDataGenerationValidator.CacheKey cacheKey =
        new BillingDataGenerationValidator.CacheKey(accountId, clusterId, startTime);
    return billingDataGenerationValidationCache.get(cacheKey);
  }

  private boolean fetchClusterBillingDataValidationInfo(String accountId, String clusterId, Instant startTime) {
    LastReceivedPublishedMessage lastReceivedPublishedMessage =
        lastReceivedPublishedMessageDao.get(accountId, clusterId);
    boolean generateData = false;
    if (null != lastReceivedPublishedMessage
        && Instant.ofEpochMilli(lastReceivedPublishedMessage.getLastReceivedAt()).isAfter(startTime)) {
      generateData = true;
    }
    if (!generateData) {
      logger.info("Not generating biling data for {} {} {} ", accountId, clusterId, startTime);
    }
    return generateData;
  }
}
