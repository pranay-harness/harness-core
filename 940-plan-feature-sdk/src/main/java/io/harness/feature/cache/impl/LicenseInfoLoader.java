package io.harness.feature.cache.impl;

import io.harness.feature.cache.LicenseCacheId;
import io.harness.feature.helpers.LicenseHelper;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;

import com.google.common.cache.CacheLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;

@Singleton
public class LicenseInfoLoader extends CacheLoader<LicenseCacheId, Optional<LicensesWithSummaryDTO>> {
  @Inject LicenseHelper licenseHelper;
  @Override
  public Optional<LicensesWithSummaryDTO> load(LicenseCacheId licenseCacheId) {
    return licenseHelper.getLicenseSummary(licenseCacheId.getAccountIdentifier(), licenseCacheId.getModuleType());
  }
}
