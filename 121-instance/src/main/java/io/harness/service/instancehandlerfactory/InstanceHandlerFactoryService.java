package io.harness.service.instancehandlerfactory;

import io.harness.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.service.InstanceHandler;

import java.util.Set;

public interface InstanceHandlerFactoryService {
  InstanceHandler getInstanceHandler(InfrastructureMapping infraMapping);

  InstanceHandler getInstanceHandlerByType(String infrastructureMappingType);

  Set<InstanceHandler> getAllInstanceHandlers();
}
