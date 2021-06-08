package io.harness.ng.activitytracker.models.apiresponses;

import io.harness.ng.activitytracker.models.ActivityHistoryByProject;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StatsDetailsByProjectResponse {
  List<ActivityHistoryByProject> activityHistoryByUserList;
}
