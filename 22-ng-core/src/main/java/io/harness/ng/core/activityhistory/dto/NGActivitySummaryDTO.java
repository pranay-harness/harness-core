package io.harness.ng.core.activityhistory.dto;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel("ActivitySummary")
public class NGActivitySummaryDTO {
  long start;
  long end;
  long heartBeatFailuresCount;
  long successfulActivitiesCount;
  long failedActivitiesCount;
}
