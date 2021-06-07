package io.harness.ng.activitytracker.models.apiresponses;

import io.harness.ng.activitytracker.models.ActivityHistoryDetails;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ActivityHistoryDetailsResponse {
  private List<ActivityHistoryDetails> activityHistoryDetailsList;
}
