package io.harness.cvng.analysis.beans;

import io.harness.beans.NGPageResponse;
import io.harness.cvng.core.beans.TimeRange;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TransactionMetricInfoSummaryPageDTO {
  NGPageResponse<TransactionMetricInfo> pageResponse;
  TimeRange deploymentTimeRange;
}
