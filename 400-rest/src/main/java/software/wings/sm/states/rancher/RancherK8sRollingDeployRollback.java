package software.wings.sm.states.rancher;

import io.harness.context.ContextElementType;

import software.wings.sm.StateType;
import software.wings.sm.states.k8s.K8sRollingDeployRollback;

public class RancherK8sRollingDeployRollback extends K8sRollingDeployRollback {
  public RancherK8sRollingDeployRollback(String name) {
    super(name);
  }

  @Override
  public ContextElementType getRequiredContextElementType() {
    return ContextElementType.RANCHER_K8S_CLUSTER_CRITERIA;
  }
}