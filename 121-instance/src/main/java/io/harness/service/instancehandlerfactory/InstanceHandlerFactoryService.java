package io.harness.service.instancehandlerfactory;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.service.InstanceHandler;

@OwnedBy(HarnessTeam.DX)
public interface InstanceHandlerFactoryService {
  InstanceHandler getInstanceHandler(InfrastructureMapping infraMapping);

  InstanceHandler getInstanceHandlerByType(String infrastructureMappingType);
}
