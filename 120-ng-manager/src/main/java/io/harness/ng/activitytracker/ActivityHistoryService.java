package io.harness.ng.activitytracker;

import io.harness.ng.activitytracker.models.apiresponses.ActivityHistoryDetailsResponse;
import io.harness.ng.activitytracker.models.apiresponses.StatsDetailsByProjectResponse;
import io.harness.ng.activitytracker.models.apiresponses.StatsDetailsByUserResponse;
import io.harness.ng.activitytracker.models.apiresponses.StatsDetailsResponse;

public interface ActivityHistoryService {
  StatsDetailsResponse getStatsDetails(String projectId, String userId, long startTime, long endTime);

  ActivityHistoryDetailsResponse getActivityHistoryDetails(
      String projectId, String userId, long startTime, long endTime);

  StatsDetailsByUserResponse getStatsDetailsByUsers(String projectId, long startTime, long endTime);

  StatsDetailsByProjectResponse getStatsDetailsByProjects(String userId, long startTime, long endTime);
}
