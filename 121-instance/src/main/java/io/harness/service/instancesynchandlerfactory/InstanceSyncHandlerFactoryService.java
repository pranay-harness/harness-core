package io.harness.service.instancesynchandlerfactory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;

@OwnedBy(HarnessTeam.DX)
public interface InstanceSyncHandlerFactoryService {
  AbstractInstanceSyncHandler getInstanceSyncHandler(String infrastructureKind);
}
