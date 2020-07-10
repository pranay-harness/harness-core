package io.harness.cvng.statemachine.entities;

import com.google.inject.Inject;

import io.harness.cvng.analysis.services.api.LogClusterService;
import io.harness.cvng.statemachine.beans.AnalysisState;

public class ServiceGuardLogClusterState extends AnalysisState {
  @Inject LogClusterService logClusterService;

  @Override
  public AnalysisState execute() {
    // TODO: To be implemented
    return null;
  }

  @Override
  public AnalysisStatus getExecutionStatus() {
    // TODO: To be implemented
    return null;
  }

  @Override
  public AnalysisState handleRerun() {
    // TODO: To be implemented
    return null;
  }

  @Override
  public AnalysisState handleRunning() {
    // TODO: To be implemented
    return null;
  }

  @Override
  public AnalysisState handleSuccess() {
    // TODO: To be implemented
    return null;
  }

  @Override
  public AnalysisState handleTransition() {
    // TODO: To be implemented
    return null;
  }

  @Override
  public AnalysisState handleRetry() {
    // TODO: To be implemented
    return null;
  }
}
