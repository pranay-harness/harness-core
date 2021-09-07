package io.harness.feature.cache.impl;

import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.feature.cache.LicenseCacheId;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;
import io.harness.licensing.remote.NgLicenseHttpClient;

import com.google.common.cache.CacheLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LicenseInfoLoader extends CacheLoader<LicenseCacheId, LicensesWithSummaryDTO> {
  @Inject NgLicenseHttpClient ngLicenseHttpClient;
  @Override
  public LicensesWithSummaryDTO load(LicenseCacheId licenseCacheId) {
    return getResponse(ngLicenseHttpClient.getLicenseSummary(
        licenseCacheId.getAccountIdentifier(), licenseCacheId.getModuleType().name()));
  }
}
