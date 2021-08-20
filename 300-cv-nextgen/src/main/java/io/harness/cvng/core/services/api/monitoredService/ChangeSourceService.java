package io.harness.cvng.core.services.api.monitoredService;

import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;

import java.util.List;
import java.util.Set;

public interface ChangeSourceService {
  void create(ServiceEnvironmentParams environmentParams, Set<ChangeSourceDTO> changeSourceDTOs);

  Set<ChangeSourceDTO> get(ServiceEnvironmentParams environmentParams, List<String> identifiers);

  void delete(ServiceEnvironmentParams environmentParams, List<String> identifiers);

  void update(ServiceEnvironmentParams environmentParams, Set<ChangeSourceDTO> changeSourceDTOs);
}
