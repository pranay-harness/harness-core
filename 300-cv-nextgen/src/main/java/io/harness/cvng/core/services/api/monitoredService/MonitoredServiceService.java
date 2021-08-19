package io.harness.cvng.core.services.api.monitoredService;

import io.harness.cvng.core.beans.HealthMonitoringFlagResponse;
import io.harness.cvng.core.beans.monitoredService.DurationDTO;
import io.harness.cvng.core.beans.monitoredService.HistoricalTrend;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceListItemDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceResponse;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.environment.dto.EnvironmentResponse;

import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import lombok.NonNull;

public interface MonitoredServiceService extends DeleteEntityByHandler<MonitoredService> {
  MonitoredServiceResponse create(String accountId, MonitoredServiceDTO monitoredServiceDTO);
  MonitoredServiceResponse update(String accountId, MonitoredServiceDTO monitoredServiceDTO);
  boolean delete(ProjectParams projectParams, String identifier);
  MonitoredServiceResponse get(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  MonitoredServiceResponse get(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, String envIdentifier);
  MonitoredServiceDTO getMonitoredServiceDTO(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  MonitoredServiceDTO getMonitoredServiceDTO(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, String envIdentifier);

  List<MonitoredService> list(
      @NonNull ProjectParams projectParams, @Nullable String serviceIdentifier, @Nullable String environmentIdentifier);

  PageResponse<MonitoredServiceListItemDTO> list(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentIdentifier, Integer offset, Integer pageSize, String filter);
  List<EnvironmentResponse> listEnvironments(String accountId, String orgIdentifier, String projectIdentifier);
  MonitoredServiceResponse createDefault(
      ProjectParams projectParams, String serviceIdentifier, String environmentIdentifier);
  HealthMonitoringFlagResponse setHealthMonitoringFlag(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean enable);

  HistoricalTrend getOverAllHealthScore(
      ProjectParams projectParams, String identifier, DurationDTO duration, Instant endTime);
}
