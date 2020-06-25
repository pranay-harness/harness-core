package io.harness.statemachine.exception;

public class AnalysisOrchestrationException extends RuntimeException {
  public AnalysisOrchestrationException(Exception e) {
    super(e);
  }

  public AnalysisOrchestrationException(String message) {
    super(message);
  }

  public AnalysisOrchestrationException(String message, Exception e) {
    super(message, e);
  }
}
