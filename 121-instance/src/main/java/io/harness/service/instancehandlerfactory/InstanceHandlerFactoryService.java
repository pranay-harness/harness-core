package io.harness.service.instancehandlerfactory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.infrastructureMapping.InfrastructureMapping;
import io.harness.service.AbstractInstanceHandler;

@OwnedBy(HarnessTeam.DX)
public interface InstanceHandlerFactoryService {
  AbstractInstanceHandler getInstanceHandler(InfrastructureMapping infraMapping);

  AbstractInstanceHandler getInstanceHandlerByType(String infrastructureMappingType);
}
