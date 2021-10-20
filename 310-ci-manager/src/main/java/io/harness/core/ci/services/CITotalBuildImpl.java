package io.harness.core.ci.services;

import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;

import com.google.inject.Inject;

public class CITotalBuildImpl implements RestrictionUsageInterface<RateLimitRestrictionMetadataDTO> {
  @Inject CIOverviewDashboardService ciOverviewDashboardService;

  @Override
  public long getCurrentValue(String accountIdentifier, RateLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    return ciOverviewDashboardService.getTotalBuild(accountIdentifier, System.currentTimeMillis()).getCount();
  }
}
