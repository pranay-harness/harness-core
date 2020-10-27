package io.harness.ng.core.activityhistory.dto;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@ApiModel("ActivitySummary")
@FieldNameConstants(innerTypeName = "NGActivitySummaryKeys")
public class NGActivitySummaryDTO {
  long startTime;
  long endTime;
  long heartBeatFailuresCount;
  long successfulActivitiesCount;
  long failedActivitiesCount;
}
