package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.TimeSeriesRecordDTO;
import io.harness.cvng.analysis.entities.ClusteredLog;
import io.harness.cvng.analysis.entities.LogAnalysisRecord;
import io.harness.cvng.analysis.entities.LogAnalysisResult;
import io.harness.cvng.analysis.entities.TimeSeriesAnomalousPatterns;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.analysis.entities.TimeSeriesRiskSummary;
import io.harness.cvng.analysis.entities.TimeSeriesShortTermHistory;

import java.time.Instant;
import java.util.List;

public interface LearningEngineDevService {
  /*
  Not to be used by anything other than LearningEngineDevResource
   */
  List<TimeSeriesRiskSummary> getRiskSummariesByTimeRange(
      String verificationTaskId, Instant startTime, Instant endTime);
  TimeSeriesAnomalousPatterns getTimeSeriesAnomalousPatternsByTaskId(String verificationTaskId);
  List<TimeSeriesCumulativeSums> getTimeSeriesCumulativeSumsByTimeRange(
      String verificationTaskId, Instant startTime, Instant endTime);
  TimeSeriesShortTermHistory getTimeSeriesShortTermHistoryByTaskId(String verificationTaskId);
  List<LogAnalysisRecord> getLogAnalysisRecordsByTimeRange(
      String verificationTaskId, Instant startTime, Instant endTime);
  List<LogAnalysisResult> getLogAnalysisResultByTimeRange(
      String verificationTaskId, Instant startTime, Instant endTime);
  List<ClusteredLog> getClusteredLogsByTimeRange(String verificationTaskId, Instant startTime, Instant endTime);
  List<TimeSeriesRecordDTO> getTimeSeriesRecordDTOs(String verificationTaskId, Instant startTime, Instant endTime);
}
