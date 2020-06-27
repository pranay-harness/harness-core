package io.harness.cvng.statemachine.services.intfc;

import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;

public interface AnalysisStateMachineService {
  void initiateStateMachine(String cvConfigId, AnalysisStateMachine stateMachine);
  void executeStateMachine(String cvConfigId);
  void executeStateMachine(AnalysisStateMachine stateMachine);
  AnalysisStateMachine getExecutingStateMachine(String cvConfigId);
  void retryStateMachineAfterFailure(AnalysisStateMachine analysisStateMachine);
  AnalysisStateMachine createStateMachine(AnalysisInput inputForAnalysis);
}
