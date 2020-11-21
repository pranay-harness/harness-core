package io.harness.cvng.analysis.beans;

import io.harness.cvng.core.beans.TimeRange;
import io.harness.ng.beans.PageResponse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TransactionMetricInfoSummaryPageDTO {
  PageResponse<TransactionMetricInfo> pageResponse;
  TimeRange deploymentTimeRange;
}
