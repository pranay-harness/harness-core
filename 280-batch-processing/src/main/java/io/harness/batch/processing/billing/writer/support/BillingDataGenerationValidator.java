/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.billing.writer.support;

import io.harness.ccm.commons.entities.batch.LastReceivedPublishedMessage;
import io.harness.ccm.health.LastReceivedPublishedMessageDao;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Get decide if need to generate billing data or not.
 */
@Component
@Slf4j
public class BillingDataGenerationValidator {
  private final LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;
  private ClusterDataGenerationValidator clusterDataGenerationValidator;

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
  public BillingDataGenerationValidator(LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao,
      ClusterDataGenerationValidator clusterDataGenerationValidator) {
    this.lastReceivedPublishedMessageDao = lastReceivedPublishedMessageDao;
    this.clusterDataGenerationValidator = clusterDataGenerationValidator;
  }

  public boolean shouldGenerateBillingData(String accountId, String clusterId, Instant startTime) {
    if (!clusterDataGenerationValidator.shouldGenerateClusterData(accountId, clusterId)) {
      return false;
    }
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
      log.info("Not generating biling data for {} {} {} ", accountId, clusterId, startTime);
    }
    return generateData;
  }
}
