/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.analysis.services.impl;

import static io.harness.cvng.analysis.CVAnalysisConstants.ANALYSIS_RISK_RESULTS_LIMIT;

import io.harness.cvng.analysis.entities.LogAnalysisResult;
import io.harness.cvng.analysis.entities.TimeSeriesRiskSummary;
import io.harness.cvng.analysis.services.api.AnalysisService;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.LogCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.beans.RiskSummaryPopoverDTO.AnalysisRisk;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AnalysisServiceImpl implements AnalysisService {
  @Inject private CVConfigService cvConfigService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Inject private LogAnalysisService logAnalysisService;

  @Override
  public List<AnalysisRisk> getTop3AnalysisRisks(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, Instant startTime, Instant endTime) {
    List<CVConfig> cvConfigs =
        cvConfigService.getCVConfigs(accountId, orgIdentifier, projectIdentifier, serviceIdentifier);
    List<String> verificationTaskIds = verificationTaskService.getServiceGuardVerificationTaskIds(
        accountId, cvConfigs.stream().map(CVConfig::getUuid).collect(Collectors.toList()));
    List<TimeSeriesRiskSummary.TransactionMetricRisk> timeSeriesMetricRisks =
        timeSeriesAnalysisService.getTopTimeSeriesTransactionMetricRisk(verificationTaskIds, startTime, endTime);

    List<AnalysisRisk> analysisRisks = new ArrayList<>();
    analysisRisks.addAll(
        timeSeriesMetricRisks.stream()
            .map(timeSeriesMetricRisk
                -> AnalysisRisk.builder()
                       .name(timeSeriesMetricRisk.getTransactionName() + " - " + timeSeriesMetricRisk.getMetricName())
                       .risk((int) (timeSeriesMetricRisk.getMetricScore() * 100))
                       .build())
            .collect(Collectors.toList()));
    List<LogAnalysisResult> logAnalysisResults =
        logAnalysisService.getTopLogAnalysisResults(verificationTaskIds, startTime, endTime);

    analysisRisks.addAll(logAnalysisResults.stream()
                             .map(logAnalysisResult
                                 -> AnalysisRisk.builder()
                                        .name(getQueryName(logAnalysisResult.getVerificationTaskId(), cvConfigs))
                                        .risk((int) (logAnalysisResult.getOverallRisk() * 100))
                                        .build())
                             .collect(Collectors.toList()));
    Collections.sort(analysisRisks, Comparator.comparingInt(AnalysisRisk::getRisk));
    return analysisRisks.subList(Math.max(0, analysisRisks.size() - ANALYSIS_RISK_RESULTS_LIMIT), analysisRisks.size());
  }

  private String getQueryName(String verificationTaskId, List<CVConfig> cvConfigs) {
    String cvConfigId = verificationTaskService.getCVConfigId(verificationTaskId);
    LogCVConfig logCVConfig =
        (LogCVConfig) cvConfigs.stream().filter(cvConfig -> cvConfig.getUuid().equals(cvConfigId)).findFirst().get();
    return logCVConfig.getQuery();
  }
}
