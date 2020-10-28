package io.harness.cvng.analysis.services.api;

import io.harness.cvng.dashboard.beans.RiskSummaryPopoverDTO;

import java.time.Instant;
import java.util.List;

public interface AnalysisService {
  List<RiskSummaryPopoverDTO.AnalysisRisk> getTop3AnalysisRisks(String accountId, String orgIdentifier,
      String projectIdentifier, String serviceIdentifier, Instant startTime, Instant endTime);
}
