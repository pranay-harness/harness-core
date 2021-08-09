package io.harness.cvng.statemachine.entities;

import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@NoArgsConstructor
@Slf4j
public class CanaryTimeSeriesAnalysisState extends TimeSeriesAnalysisState {
  @Override
  protected List<String> scheduleAnalysis(AnalysisInput analysisInput) {
    return timeSeriesAnalysisService.scheduleCanaryVerificationTaskAnalysis(analysisInput);
  }

  @Override
  public void handleFinalStatuses(AnalysisStatus finalStatus) {
    timeSeriesAnalysisService.logDeploymentVerificationProgress(getInputs(), finalStatus);
  }
}
