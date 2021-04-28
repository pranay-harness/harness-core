package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.eraro.ErrorCode.KUBERNETES_YAML_ERROR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(CDP)
public class KubernetesYamlException extends WingsException {
  private static final String REASON_ARG = "reason";

  public KubernetesYamlException(String reason) {
    this(reason, null);
  }

  public KubernetesYamlException(String reason, Throwable cause) {
    super(null, cause, KUBERNETES_YAML_ERROR, Level.ERROR, USER_SRE, EnumSet.of(FailureType.APPLICATION_ERROR));
    super.param(REASON_ARG, reason);
  }
}
