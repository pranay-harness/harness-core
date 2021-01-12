package io.harness.cvng.analysis.services.impl;

import io.harness.cvng.analysis.entities.HealthVerificationPeriod;
import io.harness.cvng.analysis.entities.LogAnalysisResult;
import io.harness.cvng.analysis.entities.TimeSeriesRiskSummary;
import io.harness.cvng.analysis.services.api.HealthVerificationService;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.services.api.HealthVerificationHeatMapService;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HealthVerificationServiceImpl implements HealthVerificationService {
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Inject private LogAnalysisService logAnalysisService;
  @Inject private CVConfigService cvConfigService;
  @Inject private HealthVerificationHeatMapService healthVerificationHeatMapService;

  @Override
  public Instant aggregateActivityAnalysis(String verificationTaskId, Instant startTime, Instant endTime,
      Instant latestTimeOfAnalysis, HealthVerificationPeriod healthVerificationPeriod) {
    String cvConfigId = verificationTaskService.getCVConfigId(verificationTaskId);
    CVConfig cvConfig = cvConfigService.get(cvConfigId);
    String serviceGuardVerificationTaskId =
        verificationTaskService.getServiceGuardVerificationTaskId(cvConfig.getAccountId(), cvConfigId);
    Double overallRisk = null;
    Instant updatedLatestTime = latestTimeOfAnalysis;
    switch (cvConfig.getVerificationType()) {
      case LOG:
        LogAnalysisResult result = logAnalysisService.getLatestAnalysisForVerificationTaskId(
            serviceGuardVerificationTaskId, startTime, endTime);
        if (result != null && result.getAnalysisEndTime().isAfter(latestTimeOfAnalysis)) {
          overallRisk = result.getOverallRisk();
          updatedLatestTime = result.getAnalysisEndTime();
        }
        break;
      case TIME_SERIES:
        TimeSeriesRiskSummary riskSummary = timeSeriesAnalysisService.getLatestTimeSeriesRiskSummary(
            serviceGuardVerificationTaskId, startTime, endTime);
        if (riskSummary != null && riskSummary.getAnalysisEndTime().isAfter(latestTimeOfAnalysis)) {
          updatedLatestTime = riskSummary.getAnalysisEndTime();
          overallRisk = riskSummary.getOverallRisk();
        }
        break;
      default:
        throw new UnsupportedOperationException("Unknown verification type of CVConfig");
    }
    if (updatedLatestTime.isAfter(latestTimeOfAnalysis)) {
      healthVerificationHeatMapService.updateRisk(
          verificationTaskId, overallRisk, updatedLatestTime, healthVerificationPeriod);
      log.info("Updated the risk for verificationTaskId {}, healthVerificationPeriod {} to {} with updateTime as {}",
          verificationTaskId, healthVerificationPeriod, overallRisk, updatedLatestTime);
    }
    return updatedLatestTime;
  }

  @Override
  public void updateProgress(
      String verificationTaskId, Instant latestTimeOfAnalysis, AnalysisStatus status, boolean isFinalState) {
    VerificationTask task = verificationTaskService.get(verificationTaskId);
    VerificationJobInstance jobInstance =
        verificationJobInstanceService.getVerificationJobInstance(task.getVerificationJobInstanceId());
    Preconditions.checkNotNull(jobInstance);
    verificationJobInstanceService.logProgress(VerificationJobInstance.AnalysisProgressLog.builder()
                                                   .analysisStatus(status)
                                                   .verificationTaskId(verificationTaskId)
                                                   .startTime(jobInstance.getPreActivityVerificationStartTime())
                                                   .endTime(latestTimeOfAnalysis)
                                                   .isFinalState(isFinalState)
                                                   .build());
  }
}
