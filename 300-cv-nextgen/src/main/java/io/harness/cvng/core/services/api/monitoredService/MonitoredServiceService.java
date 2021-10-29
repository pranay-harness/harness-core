package io.harness.cvng.core.services.api.monitoredService;

import io.harness.cvng.beans.MonitoredServiceType;
import io.harness.cvng.beans.change.ChangeCategory;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.core.beans.HealthMonitoringFlagResponse;
import io.harness.cvng.core.beans.change.ChangeSummaryDTO;
import io.harness.cvng.core.beans.monitoredService.AnomaliesSummaryDTO;
import io.harness.cvng.core.beans.monitoredService.DurationDTO;
import io.harness.cvng.core.beans.monitoredService.HealthScoreDTO;
import io.harness.cvng.core.beans.monitoredService.HistoricalTrend;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceListItemDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceResponse;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceWithHealthSources;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceDTO;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.beans.params.TimeRangeParams;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.environment.dto.EnvironmentResponse;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.NonNull;

public interface MonitoredServiceService extends DeleteEntityByHandler<MonitoredService> {
  MonitoredServiceResponse create(String accountId, MonitoredServiceDTO monitoredServiceDTO);
  MonitoredServiceResponse update(String accountId, MonitoredServiceDTO monitoredServiceDTO);
  boolean delete(ProjectParams projectParams, String identifier);
  List<MonitoredServiceResponse> get(ProjectParams projectParams, Set<String> identifier);
  MonitoredServiceResponse get(ProjectParams projectParams, String identifier);
  MonitoredServiceResponse get(ServiceEnvironmentParams serviceEnvironmentParams);
  PageResponse<MonitoredServiceResponse> getList(
      ProjectParams projectParams, String environmentIdentifier, Integer offset, Integer pageSize, String filter);
  List<MonitoredServiceWithHealthSources> getAllWithTimeSeriesHealthSources(ProjectParams projectParams);

  MonitoredServiceDTO getMonitoredServiceDTO(ServiceEnvironmentParams serviceEnvironmentParams);

  List<MonitoredService> list(
      @NonNull ProjectParams projectParams, @Nullable String serviceIdentifier, @Nullable String environmentIdentifier);

  List<MonitoredService> list(@NonNull ProjectParams projectParams, @NonNull List<String> identifiers);

  PageResponse<MonitoredServiceListItemDTO> list(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentIdentifier, Integer offset, Integer pageSize, String filter);
  List<EnvironmentResponse> listEnvironments(String accountId, String orgIdentifier, String projectIdentifier);
  MonitoredServiceResponse createDefault(
      ProjectParams projectParams, String serviceIdentifier, String environmentIdentifier);
  HealthMonitoringFlagResponse setHealthMonitoringFlag(ProjectParams projectParams, String identifier, boolean enable);

  HistoricalTrend getOverAllHealthScore(
      ProjectParams projectParams, String identifier, DurationDTO duration, Instant endTime);

  HistoricalTrend getOverAllHealthScore(
      ServiceEnvironmentParams serviceEnvironmentParams, DurationDTO duration, Instant endTime);

  HealthScoreDTO getCurrentAndDependentServicesScore(ServiceEnvironmentParams serviceEnvironmentParams);

  String getYamlTemplate(ProjectParams projectParams, MonitoredServiceType type);

  List<HealthSourceDTO> getHealthSources(ServiceEnvironmentParams serviceEnvironmentParams);
  List<ChangeEventDTO> getChangeEvents(ProjectParams projectParams, String monitoredServiceIdentifier,
      Instant startTime, Instant endTime, List<ChangeCategory> changeCategories);
  ChangeSummaryDTO getChangeSummary(
      ProjectParams projectParams, String monitoredServiceIdentifier, Instant startTime, Instant endTime);

  AnomaliesSummaryDTO getAnomaliesSummary(
      ProjectParams projectParams, String monitoredServiceIdentifier, TimeRangeParams timeRangeParams);
}
