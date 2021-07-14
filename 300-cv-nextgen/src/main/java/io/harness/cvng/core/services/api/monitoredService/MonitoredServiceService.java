package io.harness.cvng.core.services.api.monitoredService;

import io.harness.cvng.core.beans.HealthMonitoringFlagResponse;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceListItemDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceResponse;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;
import io.harness.ng.beans.PageResponse;

import java.util.List;

public interface MonitoredServiceService extends DeleteEntityByHandler<MonitoredService> {
  MonitoredServiceResponse create(String accountId, MonitoredServiceDTO monitoredServiceDTO);
  MonitoredServiceResponse update(String accountId, MonitoredServiceDTO monitoredServiceDTO);
  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  MonitoredServiceResponse get(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  MonitoredServiceResponse get(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, String envIdentifier);
  MonitoredServiceDTO getMonitoredServiceDTO(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  MonitoredServiceDTO getMonitoredServiceDTO(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, String envIdentifier);
  PageResponse<MonitoredServiceListItemDTO> list(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentIdentifier, Integer offset, Integer pageSize, String filter);
  List<String> listEnvironments(String accountId, String orgIdentifier, String projectIdentifier);
  MonitoredServiceResponse createDefault(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String environmentIdentifier);
  HealthMonitoringFlagResponse setHealthMonitoringFlag(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean enable);
}
