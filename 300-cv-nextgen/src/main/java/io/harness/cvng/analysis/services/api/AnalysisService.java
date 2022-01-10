/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.analysis.services.api;

import io.harness.cvng.dashboard.beans.RiskSummaryPopoverDTO;

import java.time.Instant;
import java.util.List;

public interface AnalysisService {
  List<RiskSummaryPopoverDTO.AnalysisRisk> getTop3AnalysisRisks(String accountId, String orgIdentifier,
      String projectIdentifier, String serviceIdentifier, Instant startTime, Instant endTime);
}
