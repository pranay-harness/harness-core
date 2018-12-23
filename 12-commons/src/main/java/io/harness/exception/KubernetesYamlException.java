package io.harness.exception;

import static io.harness.eraro.ErrorCode.KUBERNETES_YAML_ERROR;

public class KubernetesYamlException extends WingsException {
  private static final String REASON_KEY = "reason";

  public KubernetesYamlException(String reason) {
    this(reason, null);
  }

  public KubernetesYamlException(String reason, Throwable e) {
    super(KUBERNETES_YAML_ERROR, reason, e);
    super.addParam(REASON_KEY, reason);
  }
}
