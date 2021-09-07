package io.harness.cvng.core.services.impl.monitoredService;

import static io.harness.cvng.core.beans.params.ServiceEnvironmentParams.builderWithProjectParams;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.analysis.entities.LogAnalysisResult.LogAnalysisTag;
import io.harness.cvng.beans.MonitoredServiceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.ChangeSummaryDTO;
import io.harness.cvng.core.beans.HealthMonitoringFlagResponse;
import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.monitoredService.AnomaliesSummaryDTO;
import io.harness.cvng.core.beans.monitoredService.DurationDTO;
import io.harness.cvng.core.beans.monitoredService.HealthScoreDTO;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.beans.monitoredService.HistoricalTrend;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.Sources;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceListItemDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceListItemDTO.MonitoredServiceListItemDTOBuilder;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceResponse;
import io.harness.cvng.core.beans.monitoredService.RiskData;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceDTO;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.beans.params.TimeRangeParams;
import io.harness.cvng.core.beans.params.filterParams.LiveMonitoringLogAnalysisFilter;
import io.harness.cvng.core.beans.params.filterParams.TimeSeriesAnalysisFilter;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.entities.MonitoredService.MonitoredServiceKeys;
import io.harness.cvng.core.services.api.SetupUsageEventService;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.services.api.monitoredService.HealthSourceService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.core.services.api.monitoredService.ServiceDependencyService;
import io.harness.cvng.core.types.ChangeCategory;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.cvng.dashboard.services.api.LogDashboardService;
import io.harness.cvng.dashboard.services.api.TimeSeriesDashboardService;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.environment.dto.EnvironmentResponse;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.persistence.HPersistence;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.mongodb.DuplicateKeyException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

public class MonitoredServiceServiceImpl implements MonitoredServiceService {
  private static final String DEFAULT_YAML_TEMPLATE;
  private static final int BUFFER_TIME_FOR_LATEST_HEALTH_SCORE = 5;
  static {
    try {
      DEFAULT_YAML_TEMPLATE = Resources.toString(
          MonitoredServiceServiceImpl.class.getResource("monitored-service-template.yaml"), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Inject private HealthSourceService healthSourceService;
  @Inject private HPersistence hPersistence;
  @Inject private HeatMapService heatMapService;
  @Inject private NextGenService nextGenService;
  @Inject private ServiceDependencyService serviceDependencyService;
  @Inject private SetupUsageEventService setupUsageEventService;
  @Inject private ChangeSourceService changeSourceService;
  @Inject private Clock clock;
  @Inject private TimeSeriesDashboardService timeSeriesDashboardService;
  @Inject private LogDashboardService logDashboardService;

  @Override
  public MonitoredServiceResponse create(String accountId, MonitoredServiceDTO monitoredServiceDTO) {
    ServiceEnvironmentParams environmentParams = ServiceEnvironmentParams.builder()
                                                     .accountIdentifier(accountId)
                                                     .orgIdentifier(monitoredServiceDTO.getOrgIdentifier())
                                                     .projectIdentifier(monitoredServiceDTO.getProjectIdentifier())
                                                     .serviceIdentifier(monitoredServiceDTO.getServiceRef())
                                                     .environmentIdentifier(monitoredServiceDTO.getEnvironmentRef())
                                                     .build();

    validate(monitoredServiceDTO);
    checkIfAlreadyPresent(
        accountId, environmentParams, monitoredServiceDTO.getIdentifier(), monitoredServiceDTO.getSources());

    if (monitoredServiceDTO.getSources() != null) {
      healthSourceService.create(accountId, monitoredServiceDTO.getOrgIdentifier(),
          monitoredServiceDTO.getProjectIdentifier(), monitoredServiceDTO.getEnvironmentRef(),
          monitoredServiceDTO.getServiceRef(), monitoredServiceDTO.getIdentifier(),
          monitoredServiceDTO.getSources().getHealthSources(), getMonitoredServiceEnableStatus());
    }
    if (isNotEmpty(monitoredServiceDTO.getDependencies())) {
      serviceDependencyService.updateDependencies(
          environmentParams, monitoredServiceDTO.getIdentifier(), monitoredServiceDTO.getDependencies());
    }
    if (isNotEmpty(monitoredServiceDTO.getSources().getChangeSources())) {
      changeSourceService.create(environmentParams, monitoredServiceDTO.getSources().getChangeSources());
    }
    saveMonitoredServiceEntity(accountId, monitoredServiceDTO);
    setupUsageEventService.sendCreateEventsForMonitoredService(environmentParams, monitoredServiceDTO);
    return get(environmentParams, monitoredServiceDTO.getIdentifier());
  }

  private boolean getMonitoredServiceEnableStatus() {
    return true; // TODO: Need to implement this logic later based on licensing
  }

  @Override
  public MonitoredServiceResponse update(String accountId, MonitoredServiceDTO monitoredServiceDTO) {
    ServiceEnvironmentParams environmentParams = ServiceEnvironmentParams.builder()
                                                     .accountIdentifier(accountId)
                                                     .orgIdentifier(monitoredServiceDTO.getOrgIdentifier())
                                                     .projectIdentifier(monitoredServiceDTO.getProjectIdentifier())
                                                     .serviceIdentifier(monitoredServiceDTO.getServiceRef())
                                                     .environmentIdentifier(monitoredServiceDTO.getEnvironmentRef())
                                                     .build();
    MonitoredService monitoredService = getMonitoredService(environmentParams, monitoredServiceDTO.getIdentifier());
    if (monitoredService == null) {
      throw new InvalidRequestException(String.format(
          "Monitored Source Entity  with identifier %s, accountId %s, orgIdentifier %s and projectIdentifier %s  is not present",
          monitoredServiceDTO.getIdentifier(), accountId, monitoredServiceDTO.getOrgIdentifier(),
          monitoredServiceDTO.getProjectIdentifier()));
    }
    Preconditions.checkArgument(monitoredService.getServiceIdentifier().equals(monitoredServiceDTO.getServiceRef()),
        "serviceRef update is not allowed");
    Preconditions.checkArgument(
        monitoredService.getEnvironmentIdentifier().equals(monitoredServiceDTO.getEnvironmentRef()),
        "environmentRef update is not allowed");
    validate(monitoredServiceDTO);

    updateHealthSources(monitoredService, monitoredServiceDTO);
    changeSourceService.update(environmentParams, monitoredServiceDTO.getSources().getChangeSources());
    updateMonitoredService(monitoredService, monitoredServiceDTO);
    setupUsageEventService.sendCreateEventsForMonitoredService(environmentParams, monitoredServiceDTO);
    return get(environmentParams, monitoredServiceDTO.getIdentifier());
  }

  private void updateMonitoredService(MonitoredService monitoredService, MonitoredServiceDTO monitoredServiceDTO) {
    UpdateOperations<MonitoredService> updateOperations = hPersistence.createUpdateOperations(MonitoredService.class);
    updateOperations.set(MonitoredServiceKeys.name, monitoredServiceDTO.getName());
    if (monitoredServiceDTO.getDescription() != null) {
      updateOperations.set(MonitoredServiceKeys.desc, monitoredServiceDTO.getDescription());
    }
    if (monitoredServiceDTO.getTags() != null) {
      updateOperations.set(MonitoredServiceKeys.tags, TagMapper.convertToList(monitoredServiceDTO.getTags()));
    }
    if (monitoredServiceDTO.getSources() != null) {
      List<String> updatedIdentifiers = monitoredServiceDTO.getSources()
                                            .getHealthSources()
                                            .stream()
                                            .map(healthSource -> healthSource.getIdentifier())
                                            .collect(Collectors.toList());
      updateOperations.set(MonitoredServiceKeys.healthSourceIdentifiers, updatedIdentifiers);
    }
    if (isNotEmpty(monitoredServiceDTO.getDependencies())) {
      ProjectParams projectParams = ProjectParams.builder()
                                        .accountIdentifier(monitoredService.getAccountId())
                                        .orgIdentifier(monitoredService.getOrgIdentifier())
                                        .projectIdentifier(monitoredService.getProjectIdentifier())
                                        .build();
      serviceDependencyService.updateDependencies(
          projectParams, monitoredService.getIdentifier(), monitoredServiceDTO.getDependencies());
    }
    hPersistence.update(monitoredService, updateOperations);
  }

  private void updateHealthSources(MonitoredService monitoredService, MonitoredServiceDTO monitoredServiceDTO) {
    Map<String, HealthSource> currentHealthSourcesMap = new HashMap<>();
    if (monitoredServiceDTO.getSources() != null) {
      monitoredServiceDTO.getSources().getHealthSources().forEach(
          healthSource -> currentHealthSourcesMap.put(healthSource.getIdentifier(), healthSource));
    }
    List<String> existingHealthIdentifiers = monitoredService.getHealthSourceIdentifiers();
    List<String> toBeDeletedIdentifiers = existingHealthIdentifiers.stream()
                                              .filter(identifier -> !currentHealthSourcesMap.containsKey(identifier))
                                              .collect(Collectors.toList());
    healthSourceService.delete(monitoredService.getAccountId(), monitoredServiceDTO.getOrgIdentifier(),
        monitoredServiceDTO.getProjectIdentifier(), monitoredServiceDTO.getIdentifier(), toBeDeletedIdentifiers);

    Set<HealthSource> toBeCreatedHealthSources = new HashSet<>();
    Set<HealthSource> toBeUpdatedHealthSources = new HashSet<>();

    currentHealthSourcesMap.forEach((identifier, healthSource) -> {
      if (existingHealthIdentifiers.contains(identifier)) {
        toBeUpdatedHealthSources.add(healthSource);
      } else {
        toBeCreatedHealthSources.add(healthSource);
      }
    });
    healthSourceService.create(monitoredService.getAccountId(), monitoredServiceDTO.getOrgIdentifier(),
        monitoredServiceDTO.getProjectIdentifier(), monitoredService.getEnvironmentIdentifier(),
        monitoredService.getServiceIdentifier(), monitoredServiceDTO.getIdentifier(), toBeCreatedHealthSources,
        monitoredService.isEnabled());
    healthSourceService.update(monitoredService.getAccountId(), monitoredServiceDTO.getOrgIdentifier(),
        monitoredServiceDTO.getProjectIdentifier(), monitoredService.getEnvironmentIdentifier(),
        monitoredService.getServiceIdentifier(), monitoredServiceDTO.getIdentifier(), toBeUpdatedHealthSources);
  }

  @Override
  public boolean delete(ProjectParams projectParams, String identifier) {
    MonitoredService monitoredService = getMonitoredService(projectParams, identifier);
    if (monitoredService == null) {
      throw new InvalidRequestException(
          String.format("Monitored Source Entity  with identifier %s and accountId %s is not present", identifier,
              projectParams.getAccountIdentifier()));
    }
    ServiceEnvironmentParams environmentParams = builderWithProjectParams(projectParams)
                                                     .serviceIdentifier(monitoredService.getServiceIdentifier())
                                                     .environmentIdentifier(monitoredService.getEnvironmentIdentifier())
                                                     .build();
    healthSourceService.delete(projectParams.getAccountIdentifier(), projectParams.getOrgIdentifier(),
        projectParams.getProjectIdentifier(), monitoredService.getIdentifier(),
        monitoredService.getHealthSourceIdentifiers());
    serviceDependencyService.deleteDependenciesForService(projectParams, monitoredService.getIdentifier());
    changeSourceService.delete(environmentParams, monitoredService.getChangeSourceIdentifiers());
    boolean deleted = hPersistence.delete(monitoredService);
    if (deleted) {
      setupUsageEventService.sendDeleteEventsForMonitoredService(projectParams, identifier);
    }
    return deleted;
  }

  @Override
  public MonitoredServiceResponse get(ProjectParams projectParams, String identifier) {
    MonitoredService monitoredServiceEntity = getMonitoredService(projectParams, identifier);
    if (monitoredServiceEntity == null) {
      throw new InvalidRequestException(
          String.format("Monitored Source Entity with identifier %s is not present", identifier));
    }
    ServiceEnvironmentParams environmentParams =
        builderWithProjectParams(projectParams)
            .serviceIdentifier(monitoredServiceEntity.getServiceIdentifier())
            .environmentIdentifier(monitoredServiceEntity.getEnvironmentIdentifier())
            .build();
    MonitoredServiceDTO monitoredServiceDTO =
        MonitoredServiceDTO.builder()
            .name(monitoredServiceEntity.getName())
            .identifier(monitoredServiceEntity.getIdentifier())
            .orgIdentifier(monitoredServiceEntity.getOrgIdentifier())
            .projectIdentifier(monitoredServiceEntity.getProjectIdentifier())
            .environmentRef(monitoredServiceEntity.getEnvironmentIdentifier())
            .serviceRef(monitoredServiceEntity.getServiceIdentifier())
            .type(monitoredServiceEntity.getType())
            .description(monitoredServiceEntity.getDesc())
            .tags(TagMapper.convertToMap(monitoredServiceEntity.getTags()))
            .sources(
                Sources.builder()
                    .healthSources(healthSourceService.get(monitoredServiceEntity.getAccountId(),
                        monitoredServiceEntity.getOrgIdentifier(), monitoredServiceEntity.getProjectIdentifier(),
                        monitoredServiceEntity.getIdentifier(), monitoredServiceEntity.getHealthSourceIdentifiers()))
                    .changeSources(
                        changeSourceService.get(environmentParams, monitoredServiceEntity.getChangeSourceIdentifiers()))
                    .build())
            .dependencies(serviceDependencyService.getDependentServicesForMonitoredService(
                projectParams, monitoredServiceEntity.getIdentifier()))
            .build();
    return MonitoredServiceResponse.builder()
        .monitoredService(monitoredServiceDTO)
        .createdAt(monitoredServiceEntity.getCreatedAt())
        .lastModifiedAt(monitoredServiceEntity.getLastUpdatedAt())
        .build();
  }

  @Override
  public MonitoredServiceResponse get(ServiceEnvironmentParams serviceEnvironmentParams) {
    MonitoredService monitoredService = getMonitoredService(serviceEnvironmentParams);
    if (monitoredService == null) {
      return null;
    }
    return get(serviceEnvironmentParams, monitoredService.getIdentifier());
  }

  @Override
  public MonitoredServiceDTO getMonitoredServiceDTO(ServiceEnvironmentParams serviceEnvironmentParams) {
    MonitoredServiceResponse monitoredServiceResponse = get(serviceEnvironmentParams);
    if (monitoredServiceResponse == null) {
      return null;
    } else {
      return monitoredServiceResponse.getMonitoredServiceDTO();
    }
  }

  private MonitoredService getMonitoredService(ProjectParams projectParams, String identifier) {
    return hPersistence.createQuery(MonitoredService.class)
        .filter(MonitoredServiceKeys.accountId, projectParams.getAccountIdentifier())
        .filter(MonitoredServiceKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(MonitoredServiceKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .filter(MonitoredServiceKeys.identifier, identifier)
        .get();
  }

  private MonitoredService getMonitoredService(ServiceEnvironmentParams serviceEnvironmentParams) {
    return hPersistence.createQuery(MonitoredService.class)
        .filter(MonitoredServiceKeys.accountId, serviceEnvironmentParams.getAccountIdentifier())
        .filter(MonitoredServiceKeys.orgIdentifier, serviceEnvironmentParams.getOrgIdentifier())
        .filter(MonitoredServiceKeys.projectIdentifier, serviceEnvironmentParams.getProjectIdentifier())
        .filter(MonitoredServiceKeys.serviceIdentifier, serviceEnvironmentParams.getServiceIdentifier())
        .filter(MonitoredServiceKeys.environmentIdentifier, serviceEnvironmentParams.getEnvironmentIdentifier())
        .get();
  }

  private void checkIfAlreadyPresent(
      String accountId, ServiceEnvironmentParams serviceEnvironmentParams, String identifier, Sources sources) {
    MonitoredService monitoredServiceEntity = getMonitoredService(serviceEnvironmentParams, identifier);
    if (monitoredServiceEntity != null) {
      throw new DuplicateFieldException(String.format(
          "Monitored Source Entity  with identifier %s and orgIdentifier %s and projectIdentifier %s is already present",
          identifier, serviceEnvironmentParams.getOrgIdentifier(), serviceEnvironmentParams.getProjectIdentifier()));
    }
    monitoredServiceEntity =
        hPersistence.createQuery(MonitoredService.class)
            .filter(MonitoredServiceKeys.accountId, accountId)
            .filter(MonitoredServiceKeys.orgIdentifier, serviceEnvironmentParams.getOrgIdentifier())
            .filter(MonitoredServiceKeys.projectIdentifier, serviceEnvironmentParams.getProjectIdentifier())
            .filter(MonitoredServiceKeys.serviceIdentifier, serviceEnvironmentParams.getServiceIdentifier())
            .filter(MonitoredServiceKeys.environmentIdentifier, serviceEnvironmentParams.getEnvironmentIdentifier())
            .get();
    if (monitoredServiceEntity != null) {
      throw new DuplicateFieldException(String.format(
          "Monitored Source Entity  with duplicate service ref %s, environmentRef %s having identifier %s and orgIdentifier %s and projectIdentifier %s is already present",
          serviceEnvironmentParams.getServiceIdentifier(), serviceEnvironmentParams.getEnvironmentIdentifier(),
          identifier, serviceEnvironmentParams.getOrgIdentifier(), serviceEnvironmentParams.getProjectIdentifier()));
    }
    if (sources != null) {
      healthSourceService.checkIfAlreadyPresent(accountId, serviceEnvironmentParams.getOrgIdentifier(),
          serviceEnvironmentParams.getProjectIdentifier(), identifier, sources.getHealthSources());
    }
  }

  private void saveMonitoredServiceEntity(String accountId, MonitoredServiceDTO monitoredServiceDTO) {
    MonitoredService monitoredServiceEntity = MonitoredService.builder()
                                                  .name(monitoredServiceDTO.getName())
                                                  .desc(monitoredServiceDTO.getDescription())
                                                  .accountId(accountId)
                                                  .orgIdentifier(monitoredServiceDTO.getOrgIdentifier())
                                                  .projectIdentifier(monitoredServiceDTO.getProjectIdentifier())
                                                  .environmentIdentifier(monitoredServiceDTO.getEnvironmentRef())
                                                  .serviceIdentifier(monitoredServiceDTO.getServiceRef())
                                                  .identifier(monitoredServiceDTO.getIdentifier())
                                                  .type(monitoredServiceDTO.getType())
                                                  .enabled(getMonitoredServiceEnableStatus())
                                                  .tags(TagMapper.convertToList(monitoredServiceDTO.getTags()))
                                                  .build();
    if (monitoredServiceDTO.getSources() != null) {
      monitoredServiceEntity.setHealthSourceIdentifiers(monitoredServiceDTO.getSources()
                                                            .getHealthSources()
                                                            .stream()
                                                            .map(healthSourceInfo -> healthSourceInfo.getIdentifier())
                                                            .collect(Collectors.toList()));
    }
    monitoredServiceEntity.setChangeSourceIdentifiers(monitoredServiceDTO.getSources()
                                                          .getChangeSources()
                                                          .stream()
                                                          .map(changeSourceDTO -> changeSourceDTO.getIdentifier())
                                                          .collect(Collectors.toList()));
    hPersistence.save(monitoredServiceEntity);
  }

  @Override
  public List<MonitoredService> list(@NonNull ProjectParams projectParams, @Nullable String serviceIdentifier,
      @Nullable String environmentIdentifier) {
    Query<MonitoredService> query =
        hPersistence.createQuery(MonitoredService.class)
            .filter(MonitoredServiceKeys.accountId, projectParams.getAccountIdentifier())
            .filter(MonitoredServiceKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(MonitoredServiceKeys.projectIdentifier, projectParams.getProjectIdentifier());
    if (environmentIdentifier != null) {
      query.filter(MonitoredServiceKeys.environmentIdentifier, environmentIdentifier);
    }
    if (serviceIdentifier != null) {
      query.filter(MonitoredServiceKeys.serviceIdentifier, serviceIdentifier);
    }
    return query.asList();
  }

  @Override
  public PageResponse<MonitoredServiceListItemDTO> list(String accountId, String orgIdentifier,
      String projectIdentifier, String environmentIdentifier, Integer offset, Integer pageSize, String filter) {
    List<MonitoredServiceListItemDTOBuilder> monitoredServiceListItemDTOS = new ArrayList<>();
    Query<MonitoredService> monitoredServicesQuery =
        hPersistence.createQuery(MonitoredService.class)
            .filter(MonitoredServiceKeys.accountId, accountId)
            .filter(MonitoredServiceKeys.orgIdentifier, orgIdentifier)
            .filter(MonitoredServiceKeys.projectIdentifier, projectIdentifier)
            .order(Sort.descending(MonitoredServiceKeys.lastUpdatedAt));
    if (environmentIdentifier != null) {
      monitoredServicesQuery.filter(MonitoredServiceKeys.environmentIdentifier, environmentIdentifier);
    }
    List<MonitoredService> monitoredServices = monitoredServicesQuery.asList();
    Map<String, MonitoredService> idToMonitoredServiceMap =
        monitoredServices.stream().collect(Collectors.toMap(MonitoredService::getIdentifier, Function.identity()));
    if (monitoredServices != null) {
      monitoredServiceListItemDTOS =
          monitoredServices.stream()
              .filter(monitoredService
                  -> isEmpty(filter) || monitoredService.getName().toLowerCase().contains(filter.trim().toLowerCase()))
              .map(monitoredService -> toMonitorServiceListDTO(monitoredService))
              .collect(Collectors.toList());
    }
    PageResponse<MonitoredServiceListItemDTOBuilder> monitoredServiceListDTOBuilderPageResponse =
        PageUtils.offsetAndLimit(monitoredServiceListItemDTOS, offset, pageSize);

    List<Pair<String, String>> serviceEnvironmentIdentifiers = new ArrayList();
    List<String> serviceIdentifiers = new ArrayList<>();
    List<String> environmentIdentifiers = new ArrayList<>();

    for (MonitoredServiceListItemDTOBuilder monitoredServiceListDTOBuilder :
        monitoredServiceListDTOBuilderPageResponse.getContent()) {
      serviceEnvironmentIdentifiers.add(
          Pair.of(monitoredServiceListDTOBuilder.getServiceRef(), monitoredServiceListDTOBuilder.getEnvironmentRef()));
      serviceIdentifiers.add(monitoredServiceListDTOBuilder.getServiceRef());
      environmentIdentifiers.add(monitoredServiceListDTOBuilder.getEnvironmentRef());
    }
    Map<String, String> serviceIdNameMap = new HashMap<>();
    Map<String, String> environmentIdNameMap = new HashMap<>();

    nextGenService.listService(accountId, orgIdentifier, projectIdentifier, serviceIdentifiers)
        .forEach(serviceResponse
            -> serviceIdNameMap.put(
                serviceResponse.getService().getIdentifier(), serviceResponse.getService().getName()));
    nextGenService.listEnvironment(accountId, orgIdentifier, projectIdentifier, environmentIdentifiers)
        .forEach(environmentResponse
            -> environmentIdNameMap.put(
                environmentResponse.getEnvironment().getIdentifier(), environmentResponse.getEnvironment().getName()));

    List<HistoricalTrend> historicalTrendList = heatMapService.getHistoricalTrend(
        accountId, orgIdentifier, projectIdentifier, serviceEnvironmentIdentifiers, 24);
    List<RiskData> currentRiskScoreList = heatMapService.getLatestRiskScore(accountId, orgIdentifier, projectIdentifier,
        serviceEnvironmentIdentifiers, Duration.ofMinutes(BUFFER_TIME_FOR_LATEST_HEALTH_SCORE));

    List<MonitoredServiceListItemDTO> monitoredServiceListDTOS = new ArrayList<>();
    int index = 0;
    for (MonitoredServiceListItemDTOBuilder monitoredServiceListDTOBuilder :
        monitoredServiceListDTOBuilderPageResponse.getContent()) {
      String serviceName = serviceIdNameMap.get(monitoredServiceListDTOBuilder.getServiceRef());
      String environmentName = environmentIdNameMap.get(monitoredServiceListDTOBuilder.getEnvironmentRef());
      HistoricalTrend historicalTrend = historicalTrendList.get(index);
      RiskData riskData = currentRiskScoreList.get(index);
      index++;
      ServiceEnvironmentParams serviceEnvironmentParams =
          ServiceEnvironmentParams.builder()
              .accountIdentifier(accountId)
              .orgIdentifier(orgIdentifier)
              .projectIdentifier(projectIdentifier)
              .serviceIdentifier(monitoredServiceListDTOBuilder.getServiceRef())
              .environmentIdentifier(monitoredServiceListDTOBuilder.getEnvironmentRef())
              .build();
      ChangeSummaryDTO changeSummary = changeSourceService.getChangeSummary(serviceEnvironmentParams,
          idToMonitoredServiceMap.get(monitoredServiceListDTOBuilder.getIdentifier()).getChangeSourceIdentifiers(),
          clock.instant().minus(Duration.ofDays(1)), clock.instant());
      monitoredServiceListDTOS.add(monitoredServiceListDTOBuilder.historicalTrend(historicalTrend)
                                       .currentHealthScore(riskData)
                                       .serviceName(serviceName)
                                       .environmentName(environmentName)
                                       .changeSummary(changeSummary)
                                       .build());
    }
    return PageResponse.<MonitoredServiceListItemDTO>builder()
        .pageSize(pageSize)
        .pageIndex(offset)
        .totalPages(monitoredServiceListDTOBuilderPageResponse.getTotalPages())
        .totalItems(monitoredServiceListDTOBuilderPageResponse.getTotalItems())
        .pageItemCount(monitoredServiceListDTOBuilderPageResponse.getPageItemCount())
        .content(monitoredServiceListDTOS)
        .build();
  }

  @Override
  public void deleteByProjectIdentifier(
      Class<MonitoredService> clazz, String accountId, String orgIdentifier, String projectIdentifier) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    List<MonitoredService> monitoredServices = hPersistence.createQuery(MonitoredService.class)
                                                   .filter(MonitoredServiceKeys.accountId, accountId)
                                                   .filter(MonitoredServiceKeys.orgIdentifier, orgIdentifier)
                                                   .filter(MonitoredServiceKeys.projectIdentifier, projectIdentifier)
                                                   .asList();
    monitoredServices.forEach(monitoredService
        -> delete(ProjectParams.builder()
                      .accountIdentifier(monitoredService.getAccountId())
                      .orgIdentifier(monitoredService.getOrgIdentifier())
                      .projectIdentifier(monitoredService.getProjectIdentifier())
                      .build(),
            monitoredService.getIdentifier()));
  }

  @Override
  public void deleteByOrgIdentifier(Class<MonitoredService> clazz, String accountId, String orgIdentifier) {
    List<MonitoredService> monitoredServices = hPersistence.createQuery(MonitoredService.class)
                                                   .filter(MonitoredServiceKeys.accountId, accountId)
                                                   .filter(MonitoredServiceKeys.orgIdentifier, orgIdentifier)
                                                   .asList();
    monitoredServices.forEach(monitoredService
        -> delete(ProjectParams.builder()
                      .accountIdentifier(monitoredService.getAccountId())
                      .orgIdentifier(monitoredService.getOrgIdentifier())
                      .projectIdentifier(monitoredService.getProjectIdentifier())
                      .build(),
            monitoredService.getIdentifier()));
  }

  @Override
  public void deleteByAccountIdentifier(Class<MonitoredService> clazz, String accountId) {
    List<MonitoredService> monitoredServices =
        hPersistence.createQuery(MonitoredService.class).filter(MonitoredServiceKeys.accountId, accountId).asList();
    monitoredServices.forEach(monitoredService
        -> delete(ProjectParams.builder()
                      .accountIdentifier(monitoredService.getAccountId())
                      .orgIdentifier(monitoredService.getOrgIdentifier())
                      .projectIdentifier(monitoredService.getProjectIdentifier())
                      .build(),
            monitoredService.getIdentifier()));
  }

  private void validate(MonitoredServiceDTO monitoredServiceDTO) {
    if (monitoredServiceDTO.getSources() != null) {
      Set<String> identifiers = new HashSet<>();
      monitoredServiceDTO.getSources().getHealthSources().forEach(healthSource -> {
        if (!identifiers.add(healthSource.getIdentifier())) {
          throw new InvalidRequestException(String.format(
              "Multiple Health Sources exists with the same identifier %s", healthSource.getIdentifier()));
        }
        healthSource.getSpec().validate();
      });
    }
  }

  @Override
  public List<EnvironmentResponse> listEnvironments(String accountId, String orgIdentifier, String projectIdentifier) {
    List<String> environmentIdentifiers = hPersistence.createQuery(MonitoredService.class)
                                              .filter(MonitoredServiceKeys.accountId, accountId)
                                              .filter(MonitoredServiceKeys.orgIdentifier, orgIdentifier)
                                              .filter(MonitoredServiceKeys.projectIdentifier, projectIdentifier)
                                              .asList()
                                              .stream()
                                              .map(monitoredService -> monitoredService.getEnvironmentIdentifier())
                                              .collect(Collectors.toList());

    return nextGenService.listEnvironment(accountId, orgIdentifier, projectIdentifier, environmentIdentifiers)
        .stream()
        .distinct()
        .collect(Collectors.toList());
  }

  @Override
  public MonitoredServiceResponse createDefault(
      ProjectParams projectParams, String serviceIdentifier, String environmentIdentifier) {
    MonitoredServiceDTO monitoredServiceDTO = MonitoredServiceDTO.builder()
                                                  .name(serviceIdentifier + "_" + environmentIdentifier)
                                                  .identifier(serviceIdentifier + "_" + environmentIdentifier)
                                                  .orgIdentifier(projectParams.getOrgIdentifier())
                                                  .projectIdentifier(projectParams.getProjectIdentifier())
                                                  .serviceRef(serviceIdentifier)
                                                  .environmentRef(environmentIdentifier)
                                                  .type(MonitoredServiceType.APPLICATION)
                                                  .description("Default Monitored Service")
                                                  .sources(Sources.builder().build())
                                                  .dependencies(new HashSet<>())
                                                  .build();
    try {
      saveMonitoredServiceEntity(projectParams.getAccountIdentifier(), monitoredServiceDTO);
    } catch (DuplicateKeyException e) {
      monitoredServiceDTO.setIdentifier(
          monitoredServiceDTO.getIdentifier() + "_" + RandomStringUtils.randomAlphanumeric(7));
      saveMonitoredServiceEntity(projectParams.getAccountIdentifier(), monitoredServiceDTO);
    }
    setupUsageEventService.sendCreateEventsForMonitoredService(projectParams, monitoredServiceDTO);
    return get(projectParams, monitoredServiceDTO.getIdentifier());
  }

  private MonitoredServiceListItemDTOBuilder toMonitorServiceListDTO(MonitoredService monitoredService) {
    return MonitoredServiceListItemDTO.builder()
        .name(monitoredService.getName())
        .identifier(monitoredService.getIdentifier())
        .serviceRef(monitoredService.getServiceIdentifier())
        .environmentRef(monitoredService.getEnvironmentIdentifier())
        .healthMonitoringEnabled(monitoredService.isEnabled())
        .tags(TagMapper.convertToMap(monitoredService.getTags()))
        .type(monitoredService.getType());
  }

  @Override
  public HealthMonitoringFlagResponse setHealthMonitoringFlag(
      ProjectParams projectParams, String identifier, boolean enable) {
    MonitoredService monitoredService = getMonitoredService(projectParams, identifier);
    Preconditions.checkNotNull(monitoredService, "Monitored service with identifier %s does not exists", identifier);
    healthSourceService.setHealthMonitoringFlag(projectParams.getAccountIdentifier(), projectParams.getOrgIdentifier(),
        projectParams.getProjectIdentifier(), monitoredService.getIdentifier(),
        monitoredService.getHealthSourceIdentifiers(), enable);
    hPersistence.update(
        hPersistence.createQuery(MonitoredService.class).filter(MonitoredServiceKeys.uuid, monitoredService.getUuid()),
        hPersistence.createUpdateOperations(MonitoredService.class).set(MonitoredServiceKeys.enabled, enable));
    // TODO: handle race condition on same version update. Probably by using version annotation and throwing exception
    return HealthMonitoringFlagResponse.builder()
        .accountId(projectParams.getAccountIdentifier())
        .orgIdentifier(projectParams.getOrgIdentifier())
        .projectIdentifier(projectParams.getProjectIdentifier())
        .identifier(identifier)
        .healthMonitoringEnabled(enable)
        .build();
  }

  @Override
  public HistoricalTrend getOverAllHealthScore(
      ProjectParams projectParams, String identifier, DurationDTO duration, Instant endTime) {
    MonitoredService monitoredService = getMonitoredService(projectParams, identifier);
    Preconditions.checkNotNull(monitoredService, "Monitored service with identifier %s does not exists", identifier);
    return heatMapService.getOverAllHealthScore(projectParams, monitoredService.getServiceIdentifier(),
        monitoredService.getEnvironmentIdentifier(), duration, endTime);
  }

  @Override
  public HealthScoreDTO getCurrentScore(ServiceEnvironmentParams serviceEnvironmentParams) {
    List<Pair<String, String>> serviceEnvIdentifiers = Arrays.asList(
        Pair.of(serviceEnvironmentParams.getServiceIdentifier(), serviceEnvironmentParams.getEnvironmentIdentifier()));
    List<RiskData> currentRiskScoreList =
        heatMapService.getLatestRiskScore(serviceEnvironmentParams.getAccountIdentifier(),
            serviceEnvironmentParams.getOrgIdentifier(), serviceEnvironmentParams.getProjectIdentifier(),
            serviceEnvIdentifiers, Duration.ofMinutes(BUFFER_TIME_FOR_LATEST_HEALTH_SCORE));
    return HealthScoreDTO.builder().currentHealthScore(currentRiskScoreList.get(0)).build();
  }

  public String getYamlTemplate(ProjectParams projectParams) {
    // returning default yaml template, account/org/project specific templates can be generated later.
    return StringUtils.replaceEach(DEFAULT_YAML_TEMPLATE, new String[] {"$projectIdentifier", "$orgIdentifier"},
        new String[] {projectParams.getProjectIdentifier(), projectParams.getOrgIdentifier()});
  }

  @Override
  public List<HealthSourceDTO> getHealthSources(ServiceEnvironmentParams serviceEnvironmentParams) {
    MonitoredService monitoredServiceEntity = getMonitoredService(serviceEnvironmentParams);
    Set<HealthSource> healthSources = healthSourceService.get(monitoredServiceEntity.getAccountId(),
        monitoredServiceEntity.getOrgIdentifier(), monitoredServiceEntity.getProjectIdentifier(),
        monitoredServiceEntity.getIdentifier(), monitoredServiceEntity.getHealthSourceIdentifiers());
    return healthSources.stream()
        .map(healthSource -> HealthSourceDTO.toHealthSourceDTO(healthSource))
        .collect(Collectors.toList());
  }

  @Override
  public List<ChangeEventDTO> getChangeEvents(ProjectParams projectParams, String monitoredServiceIdentifier,
      Instant startTime, Instant endTime, List<ChangeCategory> changeCategories) {
    MonitoredService monitoredService = getMonitoredService(projectParams, monitoredServiceIdentifier);
    if (monitoredService == null) {
      throw new InvalidRequestException(
          String.format("Monitored Service not found for identifier %s", monitoredServiceIdentifier));
    }
    ServiceEnvironmentParams serviceEnvironmentParams =
        builderWithProjectParams(projectParams)
            .serviceIdentifier(monitoredService.getServiceIdentifier())
            .environmentIdentifier(monitoredService.getEnvironmentIdentifier())
            .build();
    return changeSourceService.getChangeEvents(
        serviceEnvironmentParams, monitoredService.getChangeSourceIdentifiers(), startTime, endTime, changeCategories);
  }

  @Override
  public ChangeSummaryDTO getChangeSummary(
      ProjectParams projectParams, String monitoredServiceIdentifier, Instant startTime, Instant endTime) {
    MonitoredService monitoredService = getMonitoredService(projectParams, monitoredServiceIdentifier);
    if (monitoredService == null) {
      throw new InvalidRequestException(
          String.format("Monitored Service not found for identifier %s", monitoredServiceIdentifier));
    }
    ServiceEnvironmentParams serviceEnvironmentParams =
        builderWithProjectParams(projectParams)
            .serviceIdentifier(monitoredService.getServiceIdentifier())
            .environmentIdentifier(monitoredService.getEnvironmentIdentifier())
            .build();
    return changeSourceService.getChangeSummary(
        serviceEnvironmentParams, monitoredService.getChangeSourceIdentifiers(), startTime, endTime);
  }

  @Override
  public AnomaliesSummaryDTO getAnomaliesSummary(
      ProjectParams projectParams, String monitoredServiceIdentifier, TimeRangeParams timeRangeParams) {
    MonitoredService monitoredService = getMonitoredService(projectParams, monitoredServiceIdentifier);
    if (monitoredService == null) {
      throw new InvalidRequestException(
          String.format("Monitored Service not found for identifier %s", monitoredServiceIdentifier));
    }
    ServiceEnvironmentParams serviceEnvironmentParams =
        ServiceEnvironmentParams.builderWithProjectParams(projectParams)
            .serviceIdentifier(monitoredService.getServiceIdentifier())
            .environmentIdentifier(monitoredService.getEnvironmentIdentifier())
            .build();
    PageParams pageParams = PageParams.builder().page(0).size(10).build();
    LiveMonitoringLogAnalysisFilter liveMonitoringLogAnalysisFilter =
        LiveMonitoringLogAnalysisFilter.builder()
            .clusterTypes(Arrays.asList(LogAnalysisTag.UNKNOWN, LogAnalysisTag.UNEXPECTED))
            .build();
    long logAnomalousCount =
        logDashboardService
            .getAllLogsData(serviceEnvironmentParams, timeRangeParams, liveMonitoringLogAnalysisFilter, pageParams)
            .getTotalItems();
    TimeSeriesAnalysisFilter timeSeriesAnalysisFilter = TimeSeriesAnalysisFilter.builder().anomalous(true).build();
    long timeSeriesAnomalousCount =
        timeSeriesDashboardService
            .getTimeSeriesMetricData(serviceEnvironmentParams, timeRangeParams, timeSeriesAnalysisFilter, pageParams)
            .getTotalItems();
    return AnomaliesSummaryDTO.builder()
        .logsAnomalies(logAnomalousCount)
        .timeSeriesAnomalies(timeSeriesAnomalousCount)
        .build();
  }
}
