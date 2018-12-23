package io.harness.exception;

import static io.harness.eraro.ErrorCode.KUBERNETES_VALUES_ERROR;

public class KubernetesValuesException extends WingsException {
  private static final String REASON_KEY = "reason";

  public KubernetesValuesException(String reason) {
    this(reason, null);
  }

  public KubernetesValuesException(String reason, Throwable e) {
    super(KUBERNETES_VALUES_ERROR, reason, e);
    super.addParam(REASON_KEY, reason);
  }
}
