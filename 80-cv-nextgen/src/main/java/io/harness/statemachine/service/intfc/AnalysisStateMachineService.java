package io.harness.statemachine.service.intfc;

import io.harness.statemachine.entity.AnalysisInput;
import io.harness.statemachine.entity.AnalysisStateMachine;

public interface AnalysisStateMachineService {
  void initiateStateMachine(String cvConfigId, AnalysisStateMachine stateMachine);
  void executeStateMachine(String cvConfigId);
  void executeStateMachine(AnalysisStateMachine stateMachine);
  AnalysisStateMachine getExecutingStateMachine(String cvConfigId);
  void retryStateMachineAfterFailure(AnalysisStateMachine analysisStateMachine);
  AnalysisStateMachine createStateMachine(AnalysisInput inputForAnalysis);
}
