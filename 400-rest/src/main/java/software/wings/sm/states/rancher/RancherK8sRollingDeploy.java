package software.wings.sm.states.rancher;

import io.harness.context.ContextElementType;

import software.wings.sm.StateType;
import software.wings.sm.states.k8s.K8sRollingDeploy;

public class RancherK8sRollingDeploy extends K8sRollingDeploy {
  public RancherK8sRollingDeploy(String name) {
    super(name, StateType.RANCHER_K8S_DEPLOYMENT_ROLLING);
  }

  @Override
  public ContextElementType getRequiredContextElementType() {
    return ContextElementType.RANCHER_K8S_CLUSTER_CRITERIA;
  }
}