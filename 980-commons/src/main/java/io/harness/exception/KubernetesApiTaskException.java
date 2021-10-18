package io.harness.exception;

import static io.harness.eraro.ErrorCode.KUBERNETES_API_TASK_EXCEPTION;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class KubernetesApiTaskException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public KubernetesApiTaskException(String message) {
    super(message, null, KUBERNETES_API_TASK_EXCEPTION, Level.ERROR, null, EnumSet.of(FailureType.APPLICATION_ERROR));
    super.param(MESSAGE_ARG, message);
  }

  public KubernetesApiTaskException(String message, FailureType failureType) {
    super(message, null, KUBERNETES_API_TASK_EXCEPTION, Level.ERROR, null, EnumSet.of(failureType));
    super.param(MESSAGE_ARG, message);
  }
}
