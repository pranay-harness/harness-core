/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.services.api;

import io.harness.cvng.dashboard.beans.RiskSummaryPopoverDTO;

import java.time.Instant;
import java.util.List;

public interface AnalysisService {
  List<RiskSummaryPopoverDTO.AnalysisRisk> getTop3AnalysisRisks(String accountId, String orgIdentifier,
      String projectIdentifier, String serviceIdentifier, Instant startTime, Instant endTime);
}
