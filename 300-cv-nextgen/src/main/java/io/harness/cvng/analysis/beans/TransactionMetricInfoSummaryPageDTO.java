package io.harness.cvng.analysis.beans;

import io.harness.cvng.core.beans.TimeRange;
import io.harness.ng.beans.PageResponse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TransactionMetricInfoSummaryPageDTO {
  PageResponse<TransactionMetricInfo> pageResponse;
  @Deprecated TimeRange deploymentTimeRange; // TODO: need to remove it in next release.
  Long deploymentStartTime;
  Long deploymentEndTime;
}
