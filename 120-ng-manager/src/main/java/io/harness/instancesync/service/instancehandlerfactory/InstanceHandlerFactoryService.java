package io.harness.instancesync.service.instancehandlerfactory;

import io.harness.instancesync.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.instancesync.service.InstanceHandler;

import java.util.Set;

public interface InstanceHandlerFactoryService {
  InstanceHandler getInstanceHandler(InfrastructureMapping infraMapping);

  Set<InstanceHandler> getAllInstanceHandlers();
}
