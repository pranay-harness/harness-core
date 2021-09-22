package io.harness.overviewdashboard.dashboardaggregateservice.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dashboards.GroupBy;
import io.harness.dashboards.SortBy;
import io.harness.overviewdashboard.dtos.CountOverview;
import io.harness.overviewdashboard.dtos.DeploymentsStatsOverview;
import io.harness.overviewdashboard.dtos.ExecutionResponse;
import io.harness.overviewdashboard.dtos.TopProjectsPanel;

@OwnedBy(HarnessTeam.PL)
public interface OverviewDashboardService {
  TopProjectsPanel getTopProjectsPanel(String accountIdentifier, String userId, long startInterval, long endInterval);

  ExecutionResponse<DeploymentsStatsOverview> getDeploymentStatsOverview(
      String accountIdentifier, String userId, long startInterval, long endInterval, GroupBy groupBy, SortBy sortBy);

  ExecutionResponse<CountOverview> getCountOverview(
      String accountIdentifier, String userId, long startInterval, long endInterval);
}
