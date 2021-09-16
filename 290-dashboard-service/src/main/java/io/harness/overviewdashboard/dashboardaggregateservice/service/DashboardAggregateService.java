package io.harness.overviewdashboard.dashboardaggregateservice.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dashboards.GroupBy;
import io.harness.dashboards.SortBy;
import io.harness.overviewdashboard.dtos.CountOverview;
import io.harness.overviewdashboard.dtos.DeploymentsStatsOverview;
import io.harness.overviewdashboard.dtos.TopProjectsPanel;

@OwnedBy(HarnessTeam.PL)
public interface DashboardAggregateService {
    TopProjectsPanel getTopProjectsPanel(String accountId, String userId, long startInterval, long endInterval);

    // MostActiveServicesList getMostActiveServicesList(String accountId, String userId, long startInterval, long endInterval, SortBy sortBy);

//  CountChangeDetails getServicesCount (String accountId, String userId, long startInterval, long endInterval);

    //  CountChangeDetails getEnvCount(
    //         String accountId, String userId, long startInterval, long endInterval);

    //  CountChangeDetails getPipelinesCount( String accountId, String userId, long startInterval, long endInterval);

    DeploymentsStatsOverview getDeploymentStatsSummary(
            String accountId, String userId, long startInterval, long endInterval, GroupBy groupBy, SortBy sortBy);

    // List<TimeBasedStats> getTimeWiseDeploymentInfo(String accountId, String userId, long startInterval, long endInterval, GroupBy groupBy);

    CountOverview getCountOverview(String accountId, String userId, long startInterval, long endInterval);
}


