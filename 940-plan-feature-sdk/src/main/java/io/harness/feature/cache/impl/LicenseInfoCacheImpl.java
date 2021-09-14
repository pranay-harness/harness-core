/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.cache.impl;

import io.harness.ModuleType;
import io.harness.feature.EnforcementConfiguration;
import io.harness.feature.cache.LicenseCacheId;
import io.harness.feature.cache.LicenseInfoCache;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class LicenseInfoCacheImpl implements LicenseInfoCache {
  private final LoadingCache<LicenseCacheId, LicensesWithSummaryDTO> cache;

  @Inject
  public LicenseInfoCacheImpl(
      LicenseInfoLoader licenseInfoCacheLoader, EnforcementConfiguration enforcementConfiguration) {
    cache = CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(enforcementConfiguration.getLicenseCacheExpiredInMinutes(), TimeUnit.MINUTES)
                .build(licenseInfoCacheLoader);
  }

  @Override
  public <T extends LicensesWithSummaryDTO> T getLicenseInfo(String accountIdentifier, ModuleType moduleType) {
    try {
      return (T) cache.get(
          LicenseCacheId.builder().accountIdentifier(accountIdentifier).moduleType(moduleType).build());
    } catch (ExecutionException e) {
      throw new IllegalStateException(String.format("Failed to load license info for account [%s] and moduleType [%s]",
                                          accountIdentifier, moduleType),
          e);
    }
  }
}
