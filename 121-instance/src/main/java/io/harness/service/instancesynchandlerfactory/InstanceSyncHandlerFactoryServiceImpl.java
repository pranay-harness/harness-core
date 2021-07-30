package io.harness.service.instancesynchandlerfactory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.DX)
public class InstanceSyncHandlerFactoryServiceImpl implements InstanceSyncHandlerFactoryService {
  @Override
  public AbstractInstanceSyncHandler getInstanceSyncHandler(final String infrastructureKind) {
    switch (infrastructureKind) {
      // TODO register the handler here
      default:
        throw new UnexpectedException(
            "No instance sync handler registered for infrastructure kind : " + infrastructureKind);
    }
  }
}
