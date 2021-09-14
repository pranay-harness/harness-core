/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureFlag;
import io.harness.beans.FeatureName;
import io.harness.pms.PmsFeatureFlagService;
import io.harness.remote.client.RestClientUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
@Singleton
public class PmsFeatureFlagHelper implements PmsFeatureFlagService {
  @Inject private AccountClient accountClient;

  private static final int CACHE_EVICTION_TIME_HOUR = 1;

  private final LoadingCache<String, Set<String>> featureFlagCache =
      CacheBuilder.newBuilder()
          .expireAfterWrite(CACHE_EVICTION_TIME_HOUR, TimeUnit.HOURS)
          .build(new CacheLoader<String, Set<String>>() {
            @Override
            public Set<String> load(@org.jetbrains.annotations.NotNull final String accountId) {
              return listAllEnabledFeatureFlagsForAccount(accountId);
            }
          });

  public boolean isEnabled(String accountId, @NotNull FeatureName featureName) {
    try {
      return featureFlagCache.get(accountId).contains(featureName.name());
    } catch (Exception e) {
      log.error("Error getting feature flags for given account: " + accountId);
      return false;
    }
  }

  public boolean isEnabled(String accountId, @NotNull String featureName) {
    try {
      return featureFlagCache.get(accountId).contains(featureName);
    } catch (Exception e) {
      log.error("Error getting feature flags for given account: " + accountId);
      return false;
    }
  }

  private Set<String> listAllEnabledFeatureFlagsForAccount(String accountId) {
    return RestClientUtils.getResponse(accountClient.listAllFeatureFlagsForAccount(accountId))
        .stream()
        .filter(FeatureFlag::isEnabled)
        .map(FeatureFlag::getName)
        .collect(Collectors.toSet());
  }

  public void updateCache(String accountId, boolean enable, String featureName) throws ExecutionException {
    if (!featureFlagCache.asMap().containsKey(accountId)) {
      return;
    }
    if (!enable) {
      featureFlagCache.get(accountId).remove(featureName);
    } else {
      featureFlagCache.get(accountId).add(featureName);
    }
  }
}
