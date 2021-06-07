package io.harness.ng.activitytracker.models.apiresponses;

import io.harness.ng.activitytracker.models.ActivityStatsPerTimestamp;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class StatsDetailsResponse {
  List<ActivityStatsPerTimestamp> activityStatsPerTimestampList;
}
