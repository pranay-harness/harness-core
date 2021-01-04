package software.wings.delegatetasks.k8s.exception;

import static io.harness.eraro.ErrorCode.KUBERNETES_CLUSTER_ERROR;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@TargetModule(Module._930_DELEGATE_TASKS)
public class K8sClusterException extends WingsException {
  private static final String REASON_ARG = "reason";

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public K8sClusterException(String reason) {
    super(null, null, KUBERNETES_CLUSTER_ERROR, Level.ERROR, null, null);
    super.param(REASON_ARG, reason);
  }
}
