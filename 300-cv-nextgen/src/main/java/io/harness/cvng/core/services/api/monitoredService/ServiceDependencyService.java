package io.harness.cvng.core.services.api.monitoredService;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.ServiceRef;
import io.harness.cvng.core.entities.ServiceDependency;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;

import java.util.Set;

@OwnedBy(CV)
public interface ServiceDependencyService extends DeleteEntityByHandler<ServiceDependency> {
  void createOrDelete(String accountId, String orgIdentifier, String projectIdentifier, String envIdentifier,
      Set<ServiceRef> fromServiceIdentifiers, String toServiceIdentifier);

  void deleteDependenciesForService(
      String accountId, String orgIdentifier, String projectIdentifier, String envIdentifier, String serviceIdentifier);

  Set<ServiceRef> getDependentServicesForMonitoredService(
      String accountId, String orgIdentifier, String projectIdentifier, String envIdentifier, String serviceIdentifier);
}
