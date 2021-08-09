package io.harness.exception;

import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;

import io.harness.eraro.Level;

public class K8sPodSyncException extends WingsException {
  public K8sPodSyncException(String message) {
    super(message, null, DEFAULT_ERROR_CODE, Level.ERROR, null, null);
  }

  public K8sPodSyncException(String message, Throwable cause) {
    super(message, cause, DEFAULT_ERROR_CODE, Level.ERROR, null, null);
  }
}
