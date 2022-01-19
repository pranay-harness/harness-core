/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl.monitoredService;

import static io.harness.cvng.core.beans.params.ServiceEnvironmentParams.builderWithProjectParams;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.entities.LogAnalysisResult.LogAnalysisTag;
import io.harness.cvng.beans.MonitoredServiceType;
import io.harness.cvng.beans.change.ChangeCategory;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.HealthMonitoringFlagResponse;
import io.harness.cvng.core.beans.change.ChangeSummaryDTO;
import io.harness.cvng.core.beans.monitoredService.AnomaliesSummaryDTO;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.monitoredService.CountServiceDTO;
import io.harness.cvng.core.beans.monitoredService.DurationDTO;
import io.harness.cvng.core.beans.monitoredService.HealthScoreDTO;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.beans.monitoredService.HistoricalTrend;
import io.harness.cvng.core.beans.monitoredService.MetricDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.ServiceDependencyDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.Sources;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceListItemDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceListItemDTO.MonitoredServiceListItemDTOBuilder;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceListItemDTO.SloHealthIndicatorDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceResponse;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceWithHealthSources;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceWithHealthSources.HealthSourceSummary;
import io.harness.cvng.core.beans.monitoredService.RiskData;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceDTO;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.MetricHealthSourceSpec;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.beans.params.TimeRangeParams;
import io.harness.cvng.core.beans.params.filterParams.LiveMonitoringLogAnalysisFilter;
import io.harness.cvng.core.beans.params.filterParams.TimeSeriesAnalysisFilter;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.entities.MonitoredService.MonitoredServiceKeys;
import io.harness.cvng.core.handler.monitoredService.BaseMonitoredServiceHandler;
import io.harness.cvng.core.services.api.SetupUsageEventService;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.services.api.monitoredService.HealthSourceService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.core.services.api.monitoredService.ServiceDependencyService;
import io.harness.cvng.core.utils.ServiceEnvKey;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.cvng.dashboard.services.api.LogDashboardService;
import io.harness.cvng.dashboard.services.api.TimeSeriesDashboardService;
import io.harness.cvng.servicelevelobjective.entities.SLOHealthIndicator;
import io.harness.cvng.servicelevelobjective.services.api.SLOHealthIndicatorService;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private static final Map<MonitoredServiceType, String> MONITORED_SERVICE_YAML_TEMPLATE = new HashMap<>();
  static {
    try {
      MONITORED_SERVICE_YAML_TEMPLATE.put(MonitoredServiceType.APPLICATION,
          Resources.toString(MonitoredServiceServiceImpl.class.getResource("monitored-service-template.yaml"),
              StandardCharsets.UTF_8));
      MONITORED_SERVICE_YAML_TEMPLATE.put(MonitoredServiceType.INFRASTRUCTURE,
          Resources.toString(MonitoredServiceServiceImpl.class.getResource("monitored-service-infra-template.yaml"),
              StandardCharsets.UTF_8));
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
  @Inject private SLOHealthIndicatorService sloHealthIndicatorService;
  @Inject private Set<BaseMonitoredServiceHandler> monitoredServiceHandlers;

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

    if (monitoredServiceDTO.getSources() != null && isNotEmpty(monitoredServiceDTO.getSources().getHealthSources())) {
      healthSourceService.create(accountId, monitoredServiceDTO.getOrgIdentifier(),
          monitoredServiceDTO.getProjectIdentifier(), monitoredServiceDTO.getEnvironmentRef(),
          monitoredServiceDTO.getServiceRef(), monitoredServiceDTO.getIdentifier(),
          monitoredServiceDTO.getSources().getHealthSources(), getMonitoredServiceEnableStatus());
    }
    if (isNotEmpty(monitoredServiceDTO.getDependencies())) {
      validateDependencyMetadata(ProjectParams.builder()
                                     .accountIdentifier(accountId)
                                     .orgIdentifier(monitoredServiceDTO.getOrgIdentifier())
                                     .projectIdentifier(monitoredServiceDTO.getProjectIdentifier())
                                     .build(),
          monitoredServiceDTO.getDependencies());
      serviceDependencyService.updateDependencies(
          environmentParams, monitoredServiceDTO.getIdentifier(), monitoredServiceDTO.getDependencies());
    }
    if (monitoredServiceDTO.getSources() != null && isNotEmpty(monitoredServiceDTO.getSources().getChangeSources())) {
      changeSourceService.create(environmentParams, monitoredServiceDTO.getSources().getChangeSources());
    }
    saveMonitoredServiceEntity(accountId, monitoredServiceDTO);
    setupUsageEventService.sendCreateEventsForMonitoredService(environmentParams, monitoredServiceDTO);
    return get(environmentParams, monitoredServiceDTO.getIdentifier());
  }

  private void validateDependencyMetadata(ProjectParams projectParams, Set<ServiceDependencyDTO> dependencyDTOs) {
    if (dependencyDTOs == null) {
      return;
    }
    dependencyDTOs.forEach(dependencyDTO -> {
      if (dependencyDTO.getDependencyMetadata() == null) {
        return;
      }
      MonitoredServiceDTO monitoredServiceDTO =
          get(projectParams, dependencyDTO.getMonitoredServiceIdentifier()).getMonitoredServiceDTO();
      Preconditions.checkNotNull(monitoredServiceDTO.getSources());
      Preconditions.checkNotNull(monitoredServiceDTO.getSources().getChangeSources());
      Set<ChangeSourceType> changeSourceTypes = monitoredServiceDTO.getSources()
                                                    .getChangeSources()
                                                    .stream()
                                                    .map(ChangeSourceDTO::getType)
                                                    .collect(Collectors.toSet());
      Set<ChangeSourceType> supportedChangeSources =
          dependencyDTO.getDependencyMetadata().getSupportedChangeSourceTypes();
      boolean isValid = false;
      for (ChangeSourceType changeSourceType : supportedChangeSources) {
        if (changeSourceTypes.contains(changeSourceType)) {
          isValid = true;
          break;
        }
      }
      if (!isValid) {
        throw new InvalidRequestException(
            "Invalid dependency setup for monitoredSource " + dependencyDTO.getMonitoredServiceIdentifier());
      }
    });
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
    MonitoredServiceDTO existingMonitoredServiceDTO =
        createMonitoredServiceDTOFromEntity(monitoredService, environmentParams).getMonitoredServiceDTO();
    monitoredServiceHandlers.forEach(baseMonitoredServiceHandler
        -> baseMonitoredServiceHandler.beforeUpdate(
            environmentParams, existingMonitoredServiceDTO, monitoredServiceDTO));
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
      List<String> updatedHealthSourceIdentifiers = monitoredServiceDTO.getSources()
                                                        .getHealthSources()
                                                        .stream()
                                                        .map(healthSource -> healthSource.getIdentifier())
                                                        .collect(Collectors.toList());
      updateOperations.set(MonitoredServiceKeys.healthSourceIdentifiers, updatedHealthSourceIdentifiers);
      List<String> updatedChangeSourceIdentifiers = monitoredServiceDTO.getSources()
                                                        .getChangeSources()
                                                        .stream()
                                                        .map(changeSource -> changeSource.getIdentifier())
                                                        .collect(Collectors.toList());
      updateOperations.set(MonitoredServiceKeys.changeSourceIdentifiers, updatedChangeSourceIdentifiers);
    }
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(monitoredService.getAccountId())
                                      .orgIdentifier(monitoredService.getOrgIdentifier())
                                      .projectIdentifier(monitoredService.getProjectIdentifier())
                                      .build();
    validateDependencyMetadata(projectParams, monitoredServiceDTO.getDependencies());
    serviceDependencyService.updateDependencies(
        projectParams, monitoredService.getIdentifier(), monitoredServiceDTO.getDependencies());

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
  public List<MonitoredServiceResponse> get(ProjectParams projectParams, Set<String> identifierSet) {
    List<MonitoredService> monitoredServices =
        hPersistence.createQuery(MonitoredService.class)
            .filter(MonitoredServiceKeys.accountId, projectParams.getAccountIdentifier())
            .filter(MonitoredServiceKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(MonitoredServiceKeys.projectIdentifier, projectParams.getProjectIdentifier())
            .field(MonitoredServiceKeys.identifier)
            .in(identifierSet)
            .asList();
    List<MonitoredServiceResponse> monitoredServiceResponseList = new ArrayList<>();
    monitoredServices.forEach(monitoredService -> {
      ServiceEnvironmentParams environmentParams =
          builderWithProjectParams(projectParams)
              .serviceIdentifier(monitoredService.getServiceIdentifier())
              .environmentIdentifier(monitoredService.getEnvironmentIdentifier())
              .build();
      monitoredServiceResponseList.add(createMonitoredServiceDTOFromEntity(monitoredService, environmentParams));
    });
    return monitoredServiceResponseList;
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

    return createMonitoredServiceDTOFromEntity(monitoredServiceEntity, environmentParams);
  }

  private MonitoredServiceResponse createMonitoredServiceDTOFromEntity(
      MonitoredService monitoredServiceEntity, ServiceEnvironmentParams environmentParams) {
    MonitoredServiceDTO monitoredServiceDTO =
        MonitoredServiceDTO.builder()
            .name(monitoredServiceEntity.getName())
            .identifier(monitoredServiceEntity.getIdentifier())
            .orgIdentifier(monitoredServiceEntity.getOrgIdentifier())
            .projectIdentifier(monitoredServiceEntity.getProjectIdentifier())
            .environmentRef(monitoredServiceEntity.getEnvironmentIdentifier())
            .environmentRefList(monitoredServiceEntity.getEnvironmentIdentifierList())
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
                ProjectParams.builder()
                    .accountIdentifier(environmentParams.getAccountIdentifier())
                    .orgIdentifier(environmentParams.getOrgIdentifier())
                    .projectIdentifier(environmentParams.getProjectIdentifier())
                    .build(),
                monitoredServiceEntity.getIdentifier()))
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
  public PageResponse<MonitoredServiceResponse> getList(
      ProjectParams projectParams, String environmentIdentifier, Integer offset, Integer pageSize, String filter) {
    List<MonitoredService> monitoredServiceEntities = getMonitoredServices(projectParams, environmentIdentifier);
    if (isEmpty(monitoredServiceEntities)) {
      throw new InvalidRequestException(
          String.format("There are no Monitored Services for the environment: %s", environmentIdentifier));
    }

    PageResponse<MonitoredService> monitoredServiceEntitiesPageResponse =
        PageUtils.offsetAndLimit(monitoredServiceEntities, offset, pageSize);

    List<MonitoredServiceResponse> monitoredServiceResponses =
        monitoredServiceEntitiesPageResponse.getContent()
            .stream()
            .map(monitoredServiceEntity -> {
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
                      .environmentRefList(monitoredServiceEntity.getEnvironmentIdentifierList())
                      .serviceRef(monitoredServiceEntity.getServiceIdentifier())
                      .type(monitoredServiceEntity.getType())
                      .description(monitoredServiceEntity.getDesc())
                      .tags(TagMapper.convertToMap(monitoredServiceEntity.getTags()))
                      .sources(
                          Sources.builder()
                              .healthSources(healthSourceService.get(monitoredServiceEntity.getAccountId(),
                                  monitoredServiceEntity.getOrgIdentifier(),
                                  monitoredServiceEntity.getProjectIdentifier(), monitoredServiceEntity.getIdentifier(),
                                  monitoredServiceEntity.getHealthSourceIdentifiers()))
                              .changeSources(changeSourceService.get(
                                  environmentParams, monitoredServiceEntity.getChangeSourceIdentifiers()))
                              .build())
                      .dependencies(serviceDependencyService.getDependentServicesForMonitoredService(
                          projectParams, monitoredServiceEntity.getIdentifier()))
                      .build();
              return MonitoredServiceResponse.builder()
                  .monitoredService(monitoredServiceDTO)
                  .createdAt(monitoredServiceEntity.getCreatedAt())
                  .lastModifiedAt(monitoredServiceEntity.getLastUpdatedAt())
                  .build();
            })
            .collect(Collectors.toList());

    return PageResponse.<MonitoredServiceResponse>builder()
        .pageSize(pageSize)
        .pageIndex(offset)
        .totalPages(monitoredServiceEntitiesPageResponse.getTotalPages())
        .totalItems(monitoredServiceEntitiesPageResponse.getTotalItems())
        .pageItemCount(monitoredServiceEntitiesPageResponse.getPageItemCount())
        .content(monitoredServiceResponses)
        .build();
  }

  @Override
  public List<MonitoredServiceWithHealthSources> getAllWithTimeSeriesHealthSources(ProjectParams projectParams) {
    List<MonitoredService> monitoredServiceEntities = getMonitoredServices(projectParams);
    if (isEmpty(monitoredServiceEntities)) {
      throw new InvalidRequestException(String.format(
          "There are no Monitored Services for the given project: %s", projectParams.getProjectIdentifier()));
    }
    Map<String, Set<HealthSource>> healthSourceMap = healthSourceService.getHealthSource(monitoredServiceEntities);
    return monitoredServiceEntities.stream()
        .map(monitoredServiceEntity -> {
          Set<HealthSource> healthSource = healthSourceMap.get(monitoredServiceEntity.getIdentifier());
          return MonitoredServiceWithHealthSources.builder()
              .name(monitoredServiceEntity.getName())
              .identifier(monitoredServiceEntity.getIdentifier())
              .healthSources(Objects.nonNull(healthSource) ? healthSource.stream()
                                                                 .map(x
                                                                     -> HealthSourceSummary.builder()
                                                                            .name(x.getName())
                                                                            .identifier(x.getIdentifier())
                                                                            .build())
                                                                 .collect(Collectors.toSet())
                                                           : Collections.emptySet())
              .build();
        })
        .collect(Collectors.toList());
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

  private List<MonitoredService> getMonitoredServices(ProjectParams projectParams, String environmentIdentifier) {
    return hPersistence.createQuery(MonitoredService.class)
        .filter(MonitoredServiceKeys.accountId, projectParams.getAccountIdentifier())
        .filter(MonitoredServiceKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(MonitoredServiceKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .filter(MonitoredServiceKeys.environmentIdentifier, environmentIdentifier)
        .asList();
  }

  private List<MonitoredService> getMonitoredServices(ProjectParams projectParams) {
    return hPersistence.createQuery(MonitoredService.class)
        .filter(MonitoredServiceKeys.accountId, projectParams.getAccountIdentifier())
        .filter(MonitoredServiceKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(MonitoredServiceKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .asList();
  }

  private List<MonitoredService> getMonitoredServicesFilteredByEnvIDAndTextAndSortedByLastUpdatedTime(
      ProjectParams projectParams, String environmentIdentifier, String filter) {
    Query<MonitoredService> monitoredServicesQuery =
        hPersistence.createQuery(MonitoredService.class)
            .filter(MonitoredServiceKeys.accountId, projectParams.getAccountIdentifier())
            .filter(MonitoredServiceKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(MonitoredServiceKeys.projectIdentifier, projectParams.getProjectIdentifier())
            .order(Sort.descending(MonitoredServiceKeys.lastUpdatedAt));
    if (environmentIdentifier != null) {
      monitoredServicesQuery.filter(MonitoredServiceKeys.environmentIdentifier, environmentIdentifier);
    }
    List<MonitoredService> monitoredServices = monitoredServicesQuery.asList();
    if (filter != null) {
      monitoredServices =
          monitoredServices.stream()
              .filter(monitoredService
                  -> isEmpty(filter) || monitoredService.getName().toLowerCase().contains(filter.trim().toLowerCase()))
              .collect(Collectors.toList());
    }
    return monitoredServices;
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
    MonitoredService monitoredServiceEntity =
        MonitoredService.builder()
            .name(monitoredServiceDTO.getName())
            .desc(monitoredServiceDTO.getDescription())
            .accountId(accountId)
            .orgIdentifier(monitoredServiceDTO.getOrgIdentifier())
            .projectIdentifier(monitoredServiceDTO.getProjectIdentifier())
            .environmentIdentifier(monitoredServiceDTO.getEnvironmentRef())
            .environmentIdentifierList(monitoredServiceDTO.getEnvironmentRefList())
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

  private HistoricalTrend getMonitoredServiceHistorialTrend(
      MonitoredService monitoredService, ProjectParams projectParams, DurationDTO duration, Instant endTime) {
    Preconditions.checkNotNull(monitoredService,
        "Monitored service for provided serviceIdentifier and envIdentifier or monitoredServiceIdentifier does not exist.");
    return heatMapService.getOverAllHealthScore(projectParams, monitoredService.getServiceIdentifier(),
        monitoredService.getEnvironmentIdentifier(), duration, endTime);
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
  public List<MonitoredService> list(@NonNull ProjectParams projectParams, @NonNull List<String> identifiers) {
    Query<MonitoredService> query =
        hPersistence.createQuery(MonitoredService.class)
            .filter(MonitoredServiceKeys.accountId, projectParams.getAccountIdentifier())
            .filter(MonitoredServiceKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(MonitoredServiceKeys.projectIdentifier, projectParams.getProjectIdentifier())
            .field(MonitoredServiceKeys.identifier)
            .in(identifiers);

    return query.asList();
  }

  private List<RiskData> getSortedDependentServiceRiskScoreList(ProjectParams projectParams,
      List<String> dependentServices, Map<ServiceEnvKey, RiskData> latestRiskScoreByServiceMap) {
    List<RiskData> dependentServiceRiskScores = new ArrayList<>();
    dependentServices.forEach(dependentService -> {
      MonitoredService dependentMonitoredService = getMonitoredService(projectParams, dependentService);
      ServiceEnvKey dependentServiceEnvKey = ServiceEnvKey.builder()
                                                 .serviceIdentifier(dependentMonitoredService.getServiceIdentifier())
                                                 .envIdentifier(dependentMonitoredService.getEnvironmentIdentifier())
                                                 .build();
      dependentServiceRiskScores.add(latestRiskScoreByServiceMap.get(dependentServiceEnvKey));
    });
    List<RiskData> sortedDependentServiceWithPositiveRiskScores =
        dependentServiceRiskScores.stream()
            .filter(x -> x.getHealthScore() != null && x.getHealthScore() >= 0)
            .collect(Collectors.toList());
    sortedDependentServiceWithPositiveRiskScores.sort(Comparator.comparing(RiskData::getHealthScore));
    List<RiskData> dependentServiceWithNegativeRiskScores =
        dependentServiceRiskScores.stream()
            .filter(x -> x.getHealthScore() == null || x.getHealthScore() < 0)
            .collect(Collectors.toList());
    sortedDependentServiceWithPositiveRiskScores.addAll(dependentServiceWithNegativeRiskScores);
    return sortedDependentServiceWithPositiveRiskScores;
  }

  private Map<ServiceEnvKey, RiskData> getLatestRiskScoreByServiceMap(
      ProjectParams projectParams, List<MonitoredService> monitoredServices) {
    List<Pair<String, String>> serviceEnvironmentIdentifiers = new ArrayList<>();
    monitoredServices.forEach(
        x -> serviceEnvironmentIdentifiers.add(Pair.of(x.getServiceIdentifier(), x.getEnvironmentIdentifier())));
    return heatMapService.getLatestRiskScoreByServiceMap(projectParams, new ArrayList<>(serviceEnvironmentIdentifiers));
  }

  private List<MonitoredService> getMonitoredServicesAtRisk(List<MonitoredService> monitoredServices,
      Map<ServiceEnvKey, RiskData> latestRiskScoreByServiceMap, boolean servicesAtRiskFilter) {
    if (servicesAtRiskFilter) {
      monitoredServices = monitoredServices.stream()
                              .filter(x -> {
                                RiskData riskData =
                                    latestRiskScoreByServiceMap.get(ServiceEnvKey.builder()
                                                                        .serviceIdentifier(x.getServiceIdentifier())
                                                                        .envIdentifier(x.getEnvironmentIdentifier())
                                                                        .build());
                                return riskData.getHealthScore() != null && riskData.getHealthScore() <= 25;
                              })
                              .collect(Collectors.toList());
    }
    return monitoredServices;
  }

  @Override
  public PageResponse<MonitoredServiceListItemDTO> list(ProjectParams projectParams, String environmentIdentifier,
      Integer offset, Integer pageSize, String filter, boolean servicesAtRiskFilter) {
    List<MonitoredService> monitoredServices = getMonitoredServicesFilteredByEnvIDAndTextAndSortedByLastUpdatedTime(
        projectParams, environmentIdentifier, filter);
    Map<String, MonitoredService> idToMonitoredServiceMap =
        monitoredServices.stream().collect(Collectors.toMap(MonitoredService::getIdentifier, Function.identity()));
    Map<ServiceEnvKey, RiskData> latestRiskScoreByServiceMap =
        getLatestRiskScoreByServiceMap(projectParams, monitoredServices);
    List<MonitoredServiceListItemDTOBuilder> monitoredServiceListItemDTOS =
        getMonitoredServicesAtRisk(monitoredServices, latestRiskScoreByServiceMap, servicesAtRiskFilter)
            .stream()
            .map(monitoredService -> toMonitorServiceListDTO(monitoredService))
            .collect(Collectors.toList());

    PageResponse<MonitoredServiceListItemDTOBuilder> monitoredServiceListDTOBuilderPageResponse =
        PageUtils.offsetAndLimit(monitoredServiceListItemDTOS, offset, pageSize);

    List<Pair<String, String>> serviceEnvironmentIdentifiers = new ArrayList<>();
    List<String> serviceIdentifiers = new ArrayList<>();
    List<String> environmentIdentifiers = new ArrayList<>();
    List<String> monitoredServiceIdentifiers = new ArrayList<>();
    for (MonitoredServiceListItemDTOBuilder monitoredServiceListDTOBuilder :
        monitoredServiceListDTOBuilderPageResponse.getContent()) {
      serviceEnvironmentIdentifiers.add(
          Pair.of(monitoredServiceListDTOBuilder.getServiceRef(), monitoredServiceListDTOBuilder.getEnvironmentRef()));
      serviceIdentifiers.add(monitoredServiceListDTOBuilder.getServiceRef());
      environmentIdentifiers.add(monitoredServiceListDTOBuilder.getEnvironmentRef());
      monitoredServiceIdentifiers.add(monitoredServiceListDTOBuilder.getIdentifier());
    }

    Map<String, String> serviceIdNameMap =
        nextGenService.getServiceIdNameMap(projectParams, new ArrayList<>(serviceIdentifiers));
    Map<String, String> environmentIdNameMap =
        nextGenService.getEnvironmentIdNameMap(projectParams, new ArrayList<>(environmentIdentifiers));
    List<HistoricalTrend> historicalTrendList = heatMapService.getHistoricalTrend(projectParams.getAccountIdentifier(),
        projectParams.getOrgIdentifier(), projectParams.getProjectIdentifier(), serviceEnvironmentIdentifiers, 24);
    Map<String, List<String>> monitoredServiceToDependentServicesMap =
        serviceDependencyService.getMonitoredServiceToDependentServicesMap(projectParams, monitoredServiceIdentifiers);

    List<MonitoredServiceListItemDTO> monitoredServiceListDTOS = new ArrayList<>();
    int index = 0;
    Map<String, List<SloHealthIndicatorDTO>> sloHealthIndicatorDTOMap =
        getSloHealthIndicators(projectParams, monitoredServiceIdentifiers);
    for (MonitoredServiceListItemDTOBuilder monitoredServiceListDTOBuilder :
        monitoredServiceListDTOBuilderPageResponse.getContent()) {
      ServiceEnvKey serviceEnvKey = ServiceEnvKey.builder()
                                        .serviceIdentifier(monitoredServiceListDTOBuilder.getServiceRef())
                                        .envIdentifier(monitoredServiceListDTOBuilder.getEnvironmentRef())
                                        .build();
      String serviceName = serviceIdNameMap.get(serviceEnvKey.getServiceIdentifier());
      String environmentName = environmentIdNameMap.get(serviceEnvKey.getEnvIdentifier());
      HistoricalTrend historicalTrend = historicalTrendList.get(index);
      RiskData monitoredServiceRiskScore = latestRiskScoreByServiceMap.get(serviceEnvKey);
      List<RiskData> dependentServiceRiskScoreList = getSortedDependentServiceRiskScoreList(projectParams,
          monitoredServiceToDependentServicesMap.get(monitoredServiceListDTOBuilder.getIdentifier()),
          latestRiskScoreByServiceMap);
      index++;
      ServiceEnvironmentParams serviceEnvironmentParams =
          ServiceEnvironmentParams.builder()
              .accountIdentifier(projectParams.getAccountIdentifier())
              .orgIdentifier(projectParams.getOrgIdentifier())
              .projectIdentifier(projectParams.getProjectIdentifier())
              .serviceIdentifier(monitoredServiceListDTOBuilder.getServiceRef())
              .environmentIdentifier(monitoredServiceListDTOBuilder.getEnvironmentRef())
              .build();

      ChangeSummaryDTO changeSummary = changeSourceService.getChangeSummary(serviceEnvironmentParams,
          idToMonitoredServiceMap.get(monitoredServiceListDTOBuilder.getIdentifier()).getChangeSourceIdentifiers(),
          clock.instant().minus(Duration.ofDays(1)), clock.instant());
      monitoredServiceListDTOS.add(
          monitoredServiceListDTOBuilder.historicalTrend(historicalTrend)
              .currentHealthScore(monitoredServiceRiskScore)
              .dependentHealthScore(dependentServiceRiskScoreList)
              .serviceName(serviceName)
              .environmentName(environmentName)
              .changeSummary(changeSummary)
              .sloHealthIndicators(sloHealthIndicatorDTOMap.get(monitoredServiceListDTOBuilder.getIdentifier()))
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
    String identifier = serviceIdentifier + "_" + environmentIdentifier;
    identifier = identifier.substring(0, Math.min(identifier.length(), 64));
    MonitoredServiceDTO monitoredServiceDTO = MonitoredServiceDTO.builder()
                                                  .name(identifier)
                                                  .identifier(identifier)
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
      identifier = identifier.substring(0, Math.min(identifier.length(), 57));
      monitoredServiceDTO.setIdentifier(identifier + "_" + RandomStringUtils.randomAlphanumeric(7));
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
    return getMonitoredServiceHistorialTrend(monitoredService, projectParams, duration, endTime);
  }

  @Override
  public HistoricalTrend getOverAllHealthScore(
      ServiceEnvironmentParams serviceEnvironmentParams, DurationDTO duration, Instant endTime) {
    MonitoredService monitoredService = getMonitoredService(serviceEnvironmentParams);
    return getMonitoredServiceHistorialTrend(monitoredService, serviceEnvironmentParams, duration, endTime);
  }

  @Override
  public HealthScoreDTO getCurrentAndDependentServicesScore(ServiceEnvironmentParams serviceEnvironmentParams) {
    List<Pair<String, String>> serviceEnvIdentifiers = new ArrayList<>(Arrays.asList(
        Pair.of(serviceEnvironmentParams.getServiceIdentifier(), serviceEnvironmentParams.getEnvironmentIdentifier())));
    MonitoredService monitoredService = getMonitoredService(serviceEnvironmentParams);
    Set<ServiceDependencyDTO> dependentServiceDTOS = serviceDependencyService.getDependentServicesForMonitoredService(
        serviceEnvironmentParams, monitoredService.getIdentifier());
    dependentServiceDTOS.forEach(dependentServiceDTO -> {
      MonitoredService dependentMonitoredService =
          getMonitoredService(serviceEnvironmentParams, dependentServiceDTO.getMonitoredServiceIdentifier());
      serviceEnvIdentifiers.add(Pair.of(
          dependentMonitoredService.getServiceIdentifier(), dependentMonitoredService.getEnvironmentIdentifier()));
    });
    List<RiskData> allServiceRiskScoreList = heatMapService.getLatestRiskScoreForAllServicesList(
        serviceEnvironmentParams.getAccountIdentifier(), serviceEnvironmentParams.getOrgIdentifier(),
        serviceEnvironmentParams.getProjectIdentifier(), serviceEnvIdentifiers);
    List<RiskData> dependentRiskScoreList = allServiceRiskScoreList.subList(1, allServiceRiskScoreList.size());
    RiskData minDependentRiskScore = null;
    if (!dependentRiskScoreList.isEmpty()) {
      List<RiskData> filteredRiskScoreList = dependentRiskScoreList.stream()
                                                 .filter(r -> r.getHealthScore() != null && r.getHealthScore() >= 0)
                                                 .collect(Collectors.toList());
      if (filteredRiskScoreList.isEmpty()) {
        minDependentRiskScore = dependentRiskScoreList.contains(RiskData.builder().riskStatus(Risk.NO_ANALYSIS).build())
            ? RiskData.builder().riskStatus(Risk.NO_ANALYSIS).build()
            : RiskData.builder().riskStatus(Risk.NO_DATA).build();
      } else {
        minDependentRiskScore = Collections.min(filteredRiskScoreList, Comparator.comparing(RiskData::getHealthScore));
      }
    }

    return HealthScoreDTO.builder()
        .currentHealthScore(allServiceRiskScoreList.get(0))
        .dependentHealthScore(minDependentRiskScore)
        .build();
  }

  public String getYamlTemplate(ProjectParams projectParams, MonitoredServiceType type) {
    // returning default yaml template, account/org/project specific templates can be generated later.
    String defaultTemplate = type == null ? MONITORED_SERVICE_YAML_TEMPLATE.get(MonitoredServiceType.APPLICATION)
                                          : MONITORED_SERVICE_YAML_TEMPLATE.get(type);
    return StringUtils.replaceEach(defaultTemplate, new String[] {"$projectIdentifier", "$orgIdentifier"},
        new String[] {projectParams.getProjectIdentifier(), projectParams.getOrgIdentifier()});
  }

  @Override
  public List<HealthSourceDTO> getHealthSources(ServiceEnvironmentParams serviceEnvironmentParams) {
    MonitoredService monitoredServiceEntity = getMonitoredService(serviceEnvironmentParams);
    Set<HealthSource> healthSources = healthSourceService.get(monitoredServiceEntity.getAccountId(),
        monitoredServiceEntity.getOrgIdentifier(), monitoredServiceEntity.getProjectIdentifier(),
        monitoredServiceEntity.getIdentifier(), monitoredServiceEntity.getHealthSourceIdentifiers());
    return healthSources.stream()
        .peek(healthSource
            -> healthSource.setIdentifier(HealthSourceService.getNameSpacedIdentifier(
                monitoredServiceEntity.getIdentifier(), healthSource.getIdentifier())))
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

  @Override
  public CountServiceDTO getCountOfServices(ProjectParams projectParams, String environmentIdentifier, String filter) {
    List<MonitoredService> allMonitoredServices = getMonitoredServicesFilteredByEnvIDAndTextAndSortedByLastUpdatedTime(
        projectParams, environmentIdentifier, filter);
    Map<ServiceEnvKey, RiskData> latestRiskScoreByServiceMap =
        getLatestRiskScoreByServiceMap(projectParams, allMonitoredServices);
    List<MonitoredService> monitoredServicesAtRisk =
        getMonitoredServicesAtRisk(allMonitoredServices, latestRiskScoreByServiceMap, true);

    return CountServiceDTO.builder()
        .allServicesCount(allMonitoredServices.size())
        .servicesAtRiskCount(monitoredServicesAtRisk.size())
        .build();
  }

  @Override
  public List<MetricDTO> getSloMetrics(
      ProjectParams projectParams, String monitoredServiceIdentifier, String healthSourceIdentifier) {
    Set<HealthSource> healthSources =
        healthSourceService.get(projectParams.getAccountIdentifier(), projectParams.getOrgIdentifier(),
            projectParams.getProjectIdentifier(), monitoredServiceIdentifier, Arrays.asList(healthSourceIdentifier));
    return healthSources.stream()
        .map(healthSource -> healthSource.getSpec())
        .filter(spec -> spec instanceof MetricHealthSourceSpec)
        .map(spec -> (MetricHealthSourceSpec) spec)
        .filter(spec -> isNotEmpty(spec.getMetricDefinitions()))
        .flatMap(spec -> spec.getMetricDefinitions().stream())
        .filter(metricDefinition -> metricDefinition.getSli().getEnabled())
        .map(metricDefinition
            -> MetricDTO.builder()
                   .metricName(metricDefinition.getMetricName())
                   .identifier(metricDefinition.getIdentifier())
                   .build())
        .collect(Collectors.toList());
  }

  @Override
  public MonitoredServiceListItemDTO getMonitoredServiceDetails(ServiceEnvironmentParams serviceEnvironmentParams) {
    ServiceEnvKey serviceEnvKey = ServiceEnvKey.builder()
                                      .serviceIdentifier(serviceEnvironmentParams.getServiceIdentifier())
                                      .envIdentifier(serviceEnvironmentParams.getEnvironmentIdentifier())
                                      .build();
    MonitoredService monitoredService = getMonitoredService(serviceEnvironmentParams);
    Preconditions.checkNotNull(monitoredService, "Monitored service with service identifier %s does not exists",
        serviceEnvKey.getServiceIdentifier());
    MonitoredServiceListItemDTOBuilder monitoredServiceListItemDTOBuilder = toMonitorServiceListDTO(monitoredService);
    Set<ServiceDependencyDTO> dependentServiceDTOs = serviceDependencyService.getDependentServicesForMonitoredService(
        serviceEnvironmentParams, monitoredService.getIdentifier());
    List<String> dependentServices = new ArrayList<>();
    dependentServiceDTOs.forEach(x -> dependentServices.add(x.getMonitoredServiceIdentifier()));
    List<MonitoredService> allMonitoredServices = getMonitoredServices(serviceEnvironmentParams);
    Map<ServiceEnvKey, RiskData> latestRiskScoreByServiceMap =
        getLatestRiskScoreByServiceMap(serviceEnvironmentParams, allMonitoredServices);

    String serviceName =
        nextGenService
            .listService(serviceEnvironmentParams.getAccountIdentifier(), serviceEnvironmentParams.getOrgIdentifier(),
                serviceEnvironmentParams.getProjectIdentifier(), Arrays.asList(serviceEnvKey.getServiceIdentifier()))
            .get(0)
            .getService()
            .getName();
    String environmentName =
        nextGenService
            .listEnvironment(serviceEnvironmentParams.getAccountIdentifier(),
                serviceEnvironmentParams.getOrgIdentifier(), serviceEnvironmentParams.getProjectIdentifier(),
                Arrays.asList(serviceEnvKey.getEnvIdentifier()))
            .get(0)
            .getEnvironment()
            .getName();
    List<HistoricalTrend> historicalTrendList =
        heatMapService.getHistoricalTrend(serviceEnvironmentParams.getAccountIdentifier(),
            serviceEnvironmentParams.getOrgIdentifier(), serviceEnvironmentParams.getProjectIdentifier(),
            Arrays.asList(Pair.of(serviceEnvKey.getServiceIdentifier(), serviceEnvKey.getEnvIdentifier())), 24);
    RiskData monitoredServiceRiskScore = latestRiskScoreByServiceMap.get(serviceEnvKey);
    List<RiskData> dependentServiceRiskScoreList = getSortedDependentServiceRiskScoreList(
        serviceEnvironmentParams, dependentServices, latestRiskScoreByServiceMap);
    ChangeSummaryDTO changeSummary = changeSourceService.getChangeSummary(serviceEnvironmentParams,
        monitoredService.getChangeSourceIdentifiers(), clock.instant().minus(Duration.ofDays(1)), clock.instant());
    Map<String, List<SloHealthIndicatorDTO>> sloHealthIndicatorDTOMap =
        getSloHealthIndicators(serviceEnvironmentParams, Collections.singletonList(monitoredService.getIdentifier()));
    return monitoredServiceListItemDTOBuilder.historicalTrend(historicalTrendList.get(0))
        .currentHealthScore(monitoredServiceRiskScore)
        .dependentHealthScore(dependentServiceRiskScoreList)
        .sloHealthIndicators(sloHealthIndicatorDTOMap.get(monitoredService.getIdentifier()))
        .serviceName(serviceName)
        .environmentName(environmentName)
        .changeSummary(changeSummary)
        .build();
  }

  private Map<String, List<SloHealthIndicatorDTO>> getSloHealthIndicators(
      ProjectParams projectParams, List<String> monitoredServiceIdentifiers) {
    Map<String, List<SloHealthIndicatorDTO>> sloHealthIndicatorDTOMap = new HashMap<>();
    if (isEmpty(monitoredServiceIdentifiers)) {
      return sloHealthIndicatorDTOMap;
    }
    List<SLOHealthIndicator> sloHealthIndicatorList =
        sloHealthIndicatorService.getByMonitoredServiceIdentifiers(projectParams, monitoredServiceIdentifiers);
    for (SLOHealthIndicator sloHealthIndicator : sloHealthIndicatorList) {
      List<SloHealthIndicatorDTO> sloHealthIndicatorDTOList =
          sloHealthIndicatorDTOMap.getOrDefault(sloHealthIndicator.getMonitoredServiceIdentifier(), new ArrayList<>());
      sloHealthIndicatorDTOList.add(
          SloHealthIndicatorDTO.builder()
              .errorBudgetRemainingPercentage(sloHealthIndicator.getErrorBudgetRemainingPercentage())
              .errorBudgetRisk(sloHealthIndicator.getErrorBudgetRisk())
              .serviceLevelObjectiveIdentifier(sloHealthIndicator.getServiceLevelObjectiveIdentifier())
              .build());
      sloHealthIndicatorDTOMap.put(sloHealthIndicator.getMonitoredServiceIdentifier(), sloHealthIndicatorDTOList);
    }
    return sloHealthIndicatorDTOMap;
  }
}
