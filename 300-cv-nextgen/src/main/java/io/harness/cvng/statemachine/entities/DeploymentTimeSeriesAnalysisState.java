package io.harness.cvng.statemachine.entities;

import io.harness.cvng.statemachine.beans.AnalysisInput;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@Slf4j
public class DeploymentTimeSeriesAnalysisState extends TimeSeriesAnalysisState {
  @Override
  protected List<String> scheduleAnalysis(AnalysisInput analysisInput) {
    return timeSeriesAnalysisService.scheduleDeploymentVerificationTaskAnalysis(analysisInput);
  }

  @Override
  public void handleFinalStatuses(AnalysisStatus finalStatus) {
    timeSeriesAnalysisService.logDeploymentVerificationProgress(getInputs(), finalStatus);
  }
}
