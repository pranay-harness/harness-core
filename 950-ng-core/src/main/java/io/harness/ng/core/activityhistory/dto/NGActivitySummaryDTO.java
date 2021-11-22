package io.harness.ng.core.activityhistory.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("ActivitySummary")
@FieldNameConstants(innerTypeName = "NGActivitySummaryKeys")
@Schema(
    name = "NGActivitySummary", description = "This is the view of the NGActivity Summary entity defined in Harness")
public class NGActivitySummaryDTO {
  // The _id is only used for aggregation logic, it won't be
  // exposed to the customers
  @JsonIgnore long _id;
  long startTime;
  long endTime;
  long heartBeatFailuresCount;
  long successfulActivitiesCount;
  long failedActivitiesCount;
}
