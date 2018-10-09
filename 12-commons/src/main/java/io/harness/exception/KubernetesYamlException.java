package io.harness.exception;

import static io.harness.eraro.ErrorCode.KUBERNETES_YAML_ERROR;

public class KubernetesYamlException extends WingsException {
  public KubernetesYamlException(String message) {
    super(KUBERNETES_YAML_ERROR, message, USER);
  }
  public KubernetesYamlException(String message, Throwable e) {
    super(KUBERNETES_YAML_ERROR, message, USER, e);
  }
}
