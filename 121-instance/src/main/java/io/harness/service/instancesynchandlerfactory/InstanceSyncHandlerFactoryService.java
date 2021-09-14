/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.instancesynchandlerfactory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;

@OwnedBy(HarnessTeam.DX)
public interface InstanceSyncHandlerFactoryService {
  AbstractInstanceSyncHandler getInstanceSyncHandler(String infrastructureKind);
}
