package io.harness.cvng.statemachine.exception;

public class AnalysisStateMachineException extends RuntimeException {
  public AnalysisStateMachineException(Exception e) {
    super(e);
  }

  public AnalysisStateMachineException(String message) {
    super(message);
  }

  public AnalysisStateMachineException(String message, Exception e) {
    super(message, e);
  }
}
