package io.harness.cvng.core.services.api.monitoredService;

import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceListDTO;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;
import io.harness.ng.beans.PageResponse;

import java.util.List;

public interface MonitoredServiceService extends DeleteEntityByHandler<MonitoredService> {
  void create(String accountId, MonitoredServiceDTO monitoredServiceDTO);
  void update(String accountId, MonitoredServiceDTO monitoredServiceDTO);
  void delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  MonitoredServiceDTO get(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  MonitoredServiceDTO get(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, String envIdentifier);
  PageResponse<MonitoredServiceListDTO> list(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentIdentifier, Integer offset, Integer pageSize, String filter);
  List<String> listEnvironments(String accountId, String orgIdentifier, String projectIdentifier);

  MonitoredServiceDTO createDefault(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String environmentIdentifier);
}
