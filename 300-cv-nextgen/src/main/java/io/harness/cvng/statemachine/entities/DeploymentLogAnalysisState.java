package io.harness.cvng.statemachine.entities;

import io.harness.cvng.statemachine.beans.AnalysisInput;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
@Data
@Builder
@Slf4j
public class DeploymentLogAnalysisState extends LogAnalysisState {
  @Override
  protected String scheduleAnalysis(AnalysisInput analysisInput) {
    return getLogAnalysisService().scheduleDeploymentLogAnalysisTask(analysisInput);
  }

  @Override
  public void handleFinalStatuses(AnalysisStatus finalStatus) {
    getLogAnalysisService().logDeploymentVerificationProgress(getInputs(), finalStatus);
  }
}
