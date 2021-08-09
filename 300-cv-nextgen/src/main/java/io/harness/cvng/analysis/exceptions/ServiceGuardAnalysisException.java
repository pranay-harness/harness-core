package io.harness.cvng.analysis.exceptions;

public class ServiceGuardAnalysisException extends RuntimeException {
  public ServiceGuardAnalysisException(Exception e) {
    super(e);
  }

  public ServiceGuardAnalysisException(String message) {
    super(message);
  }

  public ServiceGuardAnalysisException(String message, Exception e) {
    super(message, e);
  }
}
