package io.harness.cvng.core.services.api.monitoredService;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.ServiceDependencyDTO;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.entities.ServiceDependency;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;
import io.harness.cvng.core.utils.ServiceEnvKey;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;

@OwnedBy(CV)
public interface ServiceDependencyService extends DeleteEntityByHandler<ServiceDependency> {
  void updateDependencies(ProjectParams projectParams, String toMonitoredServiceIdentifier,
      Set<ServiceDependencyDTO> fromMonitoredServiceIdentifiers);

  void deleteDependenciesForService(ProjectParams projectParams, String monitoredServiceIdentifier);

  Set<ServiceDependencyDTO> getDependentServicesForMonitoredService(
      @NonNull ProjectParams projectParams, String monitoredServiceIdentifier);

  Set<ServiceDependencyDTO> getDependentServicesToMonitoredService(
      @NonNull ProjectParams projectParams, String monitoredServiceIdentifier);

  List<ServiceDependency> getServiceDependencies(
      @NonNull ProjectParams projectParams, @NonNull List<String> monitoredServiceIdentifiers);

  Map<ServiceEnvKey, List<String>> getMonitoredServiceToDependentServicesMap(
      @NonNull ProjectParams projectParams, List<MonitoredService> monitoredServices);
}
