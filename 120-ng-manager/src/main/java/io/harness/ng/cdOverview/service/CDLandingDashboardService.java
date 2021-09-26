package io.harness.ng.cdOverview.service;

import io.harness.dashboards.ServicesDashboardInfo;
import io.harness.dashboards.SortBy;
import io.harness.ng.core.OrgProjectIdentifier;

import java.util.List;
import javax.validation.constraints.NotNull;

public interface CDLandingDashboardService {
  ServicesDashboardInfo getActiveServices(@NotNull String accountIdentifier,
      @NotNull List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval,
      @NotNull SortBy sortBy);
}
