package io.harness.cvng.core.services.api.monitoredService;

import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;

public interface MonitoredServiceService extends DeleteEntityByHandler<MonitoredService> {
  void create(String accountId, MonitoredServiceDTO monitoredServiceDTO);
  void update(String accountId, MonitoredServiceDTO monitoredServiceDTO);
  void delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  MonitoredServiceDTO get(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
}
