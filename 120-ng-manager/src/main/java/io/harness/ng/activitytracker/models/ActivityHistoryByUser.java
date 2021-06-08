package io.harness.ng.activitytracker.models;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ActivityHistoryByUser {
  String userId;
  List<ActivityStatsPerTimestamp> activityStatsPerTimestampList;
}
