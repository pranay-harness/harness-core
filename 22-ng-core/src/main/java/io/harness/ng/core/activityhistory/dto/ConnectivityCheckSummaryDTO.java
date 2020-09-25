package io.harness.ng.core.activityhistory.dto;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel("ConnectivityCheckSummary")
public class ConnectivityCheckSummaryDTO implements ActivityDetail {
  long failureCount;
}
