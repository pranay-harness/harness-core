package io.harness.ng.activitytracker.models;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ActivityStatsPerTimestamp {
  long timestamp;
  List<CountPerActivityType> countPerActivityTypeList;
  long totalCount;
}
