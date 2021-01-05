package io.harness.cvng.statemachine.services.intfc;

import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;

import java.util.List;
import java.util.Optional;

public interface AnalysisStateMachineService {
  void initiateStateMachine(String verificationTaskId, AnalysisStateMachine stateMachine);
  void executeStateMachine(String verificationTaskId);
  Optional<AnalysisStateMachine> ignoreOldStatemachine(AnalysisStateMachine analysisStateMachine);
  void executeStateMachine(AnalysisStateMachine stateMachine);
  AnalysisStateMachine getExecutingStateMachine(String verificationTaskId);
  void retryStateMachineAfterFailure(AnalysisStateMachine analysisStateMachine);
  AnalysisStateMachine createStateMachine(AnalysisInput inputForAnalysis);
  void save(List<AnalysisStateMachine> stateMachineList);
}
