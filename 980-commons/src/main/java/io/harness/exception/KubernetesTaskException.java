package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.eraro.ErrorCode.KUBERNETES_TASK_EXCEPTION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(CDP)
public class KubernetesTaskException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public KubernetesTaskException(String message) {
    super(message, null, KUBERNETES_TASK_EXCEPTION, Level.ERROR, null, EnumSet.of(FailureType.APPLICATION_ERROR));
    super.param(MESSAGE_ARG, message);
  }

  public KubernetesTaskException(String message, FailureType failureType) {
    super(message, null, KUBERNETES_TASK_EXCEPTION, Level.ERROR, null, EnumSet.of(failureType));
    super.param(MESSAGE_ARG, message);
  }
}
