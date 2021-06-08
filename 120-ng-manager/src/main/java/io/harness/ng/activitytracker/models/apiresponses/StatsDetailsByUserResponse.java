package io.harness.ng.activitytracker.models.apiresponses;

import io.harness.ng.activitytracker.models.ActivityHistoryByUser;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StatsDetailsByUserResponse {
  List<ActivityHistoryByUser> activityHistoryByUserList;
}
