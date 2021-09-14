/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
