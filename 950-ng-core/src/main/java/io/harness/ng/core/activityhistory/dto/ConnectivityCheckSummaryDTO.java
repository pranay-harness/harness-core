package io.harness.ng.core.activityhistory.dto;

import io.harness.ng.core.activityhistory.NGActivityStatus;

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
@ApiModel("ConnectivityCheckSummary")
@FieldNameConstants(innerTypeName = "ConnectivityCheckSummaryKeys")
public class ConnectivityCheckSummaryDTO {
  long successCount;
  long failureCount;
  long startTime;
  long endTime;
}
