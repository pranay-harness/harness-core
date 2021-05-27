package io.harness.core.ci.services;

import io.harness.app.beans.entities.BuildActiveInfo;
import io.harness.app.beans.entities.BuildFailureInfo;
import io.harness.app.beans.entities.BuildHealth;
import io.harness.app.beans.entities.DashboardBuildExecutionInfo;
import io.harness.app.beans.entities.DashboardBuildRepositoryInfo;
import io.harness.app.beans.entities.DashboardBuildsHealthInfo;

import java.util.List;

public interface CIOverviewDashboardService {
  BuildHealth getCountAndRate(long currentCount, long previousCount);

  DashboardBuildsHealthInfo getDashBoardBuildHealthInfoWithRate(String accountId, String orgId, String projectId,
      long startInterval, long endInterval, long previousStartInterval);

  DashboardBuildExecutionInfo getBuildExecutionBetweenIntervals(
      String accountId, String orgId, String projectId, long startInterval, long endInterval);

  List<BuildFailureInfo> getDashboardBuildFailureInfo(String accountId, String orgId, String projectId, long days);

  List<BuildActiveInfo> getDashboardBuildActiveInfo(String accountId, String orgId, String projectId, long days);

  DashboardBuildRepositoryInfo getDashboardBuildRepository(String accountId, String orgId, String projectId,
      long startInterval, long endInterval, long previousStartInterval);
}
