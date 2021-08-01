package io.harness.service.instancesynchandlerfactory;

import static io.harness.cdng.infra.yaml.InfrastructureKind.KUBERNETES_DIRECT;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;
import io.harness.service.instancesynchandler.K8sInstanceSyncHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.DX)
public class InstanceSyncHandlerFactoryServiceImpl implements InstanceSyncHandlerFactoryService {
  private final K8sInstanceSyncHandler k8sInstanceSyncHandler;

  @Inject
  public InstanceSyncHandlerFactoryServiceImpl(K8sInstanceSyncHandler k8sInstanceSyncHandler) {
    this.k8sInstanceSyncHandler = k8sInstanceSyncHandler;
  }

  @Override
  public AbstractInstanceSyncHandler getInstanceSyncHandler(final String infrastructureKind) {
    switch (infrastructureKind) {
      case KUBERNETES_DIRECT:
        return k8sInstanceSyncHandler;
      default:
        throw new UnexpectedException(
            "No instance sync handler registered for infrastructure kind: " + infrastructureKind);
    }
  }
}
