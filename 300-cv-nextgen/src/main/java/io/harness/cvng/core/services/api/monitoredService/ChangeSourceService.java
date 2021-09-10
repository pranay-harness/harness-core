package io.harness.cvng.core.services.api.monitoredService;

import io.harness.cvng.core.beans.ChangeSummaryDTO;
import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.ChangeSource;
import io.harness.cvng.core.entities.changeSource.KubernetesChangeSource;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;
import io.harness.cvng.core.types.ChangeCategory;
import io.harness.cvng.core.types.ChangeSourceType;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface ChangeSourceService extends DeleteEntityByHandler<ChangeSource> {
  void create(ServiceEnvironmentParams environmentParams, Set<ChangeSourceDTO> changeSourceDTOs);

  Set<ChangeSourceDTO> get(ServiceEnvironmentParams environmentParams, List<String> identifiers);

  Set<ChangeSourceDTO> getByType(ServiceEnvironmentParams environmentParams, ChangeSourceType changeSourceType);

  void delete(ServiceEnvironmentParams environmentParams, List<String> identifiers);

  void update(ServiceEnvironmentParams environmentParams, Set<ChangeSourceDTO> changeSourceDTOs);

  void enqueueDataCollectionTask(KubernetesChangeSource changeSource);

  List<ChangeEventDTO> getChangeEvents(ServiceEnvironmentParams serviceEnvironmentParams,
      List<String> changeSourceIdentifiers, Instant startTime, Instant endTime, List<ChangeCategory> changeCategories);

  ChangeSummaryDTO getChangeSummary(ServiceEnvironmentParams serviceEnvironmentParams,
      List<String> changeSourceIdentifiers, Instant startTime, Instant endTime);
}
