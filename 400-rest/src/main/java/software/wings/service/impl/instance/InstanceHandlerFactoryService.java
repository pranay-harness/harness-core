package software.wings.service.impl.instance;

import software.wings.beans.InfrastructureMapping;

import java.util.Set;

public interface InstanceHandlerFactoryService {
  InstanceHandler getInstanceHandler(InfrastructureMapping infraMapping);

  Set<InstanceHandler> getAllInstanceHandlers();
}
