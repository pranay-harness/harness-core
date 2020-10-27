package io.harness.ng.core.activityhistory.dto;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@ApiModel("ConnectivityCheckSummary")
@FieldNameConstants(innerTypeName = "ConnectivityCheckSummaryKeys")
public class ConnectivityCheckSummaryDTO {
  long failureCount;
}
