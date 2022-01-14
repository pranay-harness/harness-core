package io.harness.service.instancesynchandlerfactory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.service.beans.ServiceSpecType;
import io.harness.exception.UnexpectedException;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;
import io.harness.service.instancesynchandler.K8sInstanceSyncHandler;
import io.harness.service.instancesynchandler.NativeHelmInstanceSyncHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class InstanceSyncHandlerFactoryServiceImpl implements InstanceSyncHandlerFactoryService {
  private final K8sInstanceSyncHandler k8sInstanceSyncHandler;
  private final NativeHelmInstanceSyncHandler nativeHelmInstanceSyncHandler;
  @Override
  public AbstractInstanceSyncHandler getInstanceSyncHandler(final String deploymentType) {
    switch (deploymentType) {
      case ServiceSpecType.KUBERNETES:
        return k8sInstanceSyncHandler;
      case ServiceSpecType.NATIVE_HELM:
        return nativeHelmInstanceSyncHandler;
      default:
        throw new UnexpectedException("No instance sync handler registered for infrastructure kind: " + deploymentType);
    }
  }
}
