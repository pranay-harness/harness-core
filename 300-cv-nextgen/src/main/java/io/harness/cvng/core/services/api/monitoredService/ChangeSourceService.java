package io.harness.cvng.core.services.api.monitoredService;

import io.harness.cvng.core.beans.EnvironmentParams;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;

import java.util.List;
import java.util.Set;

public interface ChangeSourceService {
  void create(EnvironmentParams environmentParams, Set<ChangeSourceDTO> changeSourceDTOs);

  Set<ChangeSourceDTO> get(EnvironmentParams environmentParams, List<String> identifiers);

  void delete(EnvironmentParams environmentParams, List<String> identifiers);

  void update(EnvironmentParams environmentParams, Set<ChangeSourceDTO> changeSourceDTOs);
}
