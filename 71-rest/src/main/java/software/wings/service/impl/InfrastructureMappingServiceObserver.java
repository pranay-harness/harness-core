package software.wings.service.impl;

import software.wings.beans.InfrastructureMapping;

public interface InfrastructureMappingServiceObserver {
  void onSaved(InfrastructureMapping infrastructureMapping);
  void onUpdated(InfrastructureMapping infrastructureMapping);
}
