package io.harness.cvng.core.services.api.monitoredService;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.ProjectParams;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.ServiceRef;
import io.harness.cvng.core.entities.ServiceDependency;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.NonNull;

@OwnedBy(CV)
public interface ServiceDependencyService extends DeleteEntityByHandler<ServiceDependency> {
  void updateDependencies(String accountId, String orgIdentifier, String projectIdentifier, String toServiceIdentifier,
      String envIdentifier, Set<ServiceRef> fromServiceIdentifiers);

  void deleteDependenciesForService(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, String envIdentifier);

  Set<ServiceRef> getDependentServicesForMonitoredService(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, String envIdentifier);

  List<ServiceDependency> getServiceDependencies(
      @NonNull ProjectParams projectParams, @Nullable String serviceIdentifier, @Nullable String envIdentifier);
}
