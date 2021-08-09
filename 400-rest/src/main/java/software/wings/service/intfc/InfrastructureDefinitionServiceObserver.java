package software.wings.service.intfc;

import software.wings.infra.InfrastructureDefinition;

public interface InfrastructureDefinitionServiceObserver {
  void onSaved(InfrastructureDefinition infrastructureDefinition);
  void onUpdated(InfrastructureDefinition infrastructureDefinition);
}
