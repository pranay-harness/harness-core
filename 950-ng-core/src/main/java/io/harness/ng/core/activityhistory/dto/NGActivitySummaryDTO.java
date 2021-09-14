/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.activityhistory.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
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
