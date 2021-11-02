package io.harness.core.ci.services;

import io.harness.enforcement.beans.metadata.StaticLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;

import com.google.inject.Inject;

public class CITotalBuildImpl implements RestrictionUsageInterface<StaticLimitRestrictionMetadataDTO> {
  @Inject CIOverviewDashboardService ciOverviewDashboardService;

  @Override
  public long getCurrentValue(String accountIdentifier, StaticLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    return ciOverviewDashboardService.getTotalBuild(accountIdentifier).getCount();
  }
}
