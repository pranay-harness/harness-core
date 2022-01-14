package io.harness.ng.core.activityhistory.dto;

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
@ApiModel("ConnectivityCheckSummary")
@FieldNameConstants(innerTypeName = "ConnectivityCheckSummaryKeys")
@Schema(name = "ConnectivityCheckSummary",
    description = "This is the view of the Connectivity Check Summary entity defined in Harness")
public class ConnectivityCheckSummaryDTO {
  long successCount;
  long failureCount;
  long startTime;
  long endTime;
}
