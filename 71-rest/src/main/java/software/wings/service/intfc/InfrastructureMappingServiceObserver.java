package software.wings.service.intfc;

import software.wings.beans.InfrastructureMapping;

public interface InfrastructureMappingServiceObserver {
  void onSaved(InfrastructureMapping infrastructureMapping);
  void onUpdated(InfrastructureMapping infrastructureMapping);
}