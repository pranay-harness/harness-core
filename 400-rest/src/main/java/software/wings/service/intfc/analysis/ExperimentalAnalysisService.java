/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.analysis;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;

import software.wings.service.impl.analysis.ExpAnalysisInfo;
import software.wings.service.impl.analysis.ExperimentPerformance;
import software.wings.service.impl.analysis.ExperimentStatus;
import software.wings.service.impl.analysis.ExperimentalMessageComparisonResult;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.newrelic.ExperimentalMetricRecord;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

public interface ExperimentalAnalysisService {
  ExperimentPerformance getMetricExpAnalysisPerformance(PageRequest<ExperimentalMetricAnalysisRecord> pageRequest);

  PageResponse<ExpAnalysisInfo> getMetricExpAnalysisInfoList(PageRequest<ExperimentalMetricAnalysisRecord> pageRequest);

  List<ExpAnalysisInfo> getLogExpAnalysisInfoList();

  ExperimentalMetricRecord markExperimentStatus(
      String stateExecutionId, StateType stateType, String experimentName, ExperimentStatus experimentStatus);

  ExperimentalMetricRecord getExperimentalMetricAnalysisSummary(
      String stateExecutionId, StateType stateType, String expName);

  void updateMismatchStatusForExperimentalRecord(String stateExecutionId, Integer analysisMinute);

  LogMLAnalysisSummary getExperimentalAnalysisSummary(
      String stateExecutionId, String appId, StateType stateType, String expName);

  List<ExperimentalMessageComparisonResult> getMessagePairsToVote(String serviceId);
  boolean saveMessagePairsToVote(String serviceId, Map<String, String> userVotes);
}
