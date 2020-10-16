package io.harness.cvng.statemachine.services.intfc;

import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;

public interface AnalysisStateMachineService {
  void initiateStateMachine(String verificationTaskId, AnalysisStateMachine stateMachine);
  void executeStateMachine(String verificationTaskId);
  void executeStateMachine(AnalysisStateMachine stateMachine);
  AnalysisStateMachine getExecutingStateMachine(String verificationTaskId);
  void retryStateMachineAfterFailure(AnalysisStateMachine analysisStateMachine);
  AnalysisStateMachine createStateMachine(AnalysisInput inputForAnalysis);
}
