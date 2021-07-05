package io.harness.cvng.core.services.impl.monitoredService;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.cvng.beans.MonitoredServiceType;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.Sources;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceListDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceResponse;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.entities.MonitoredService.MonitoredServiceKeys;
import io.harness.cvng.core.services.api.monitoredService.HealthSourceService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.mongodb.DuplicateKeyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

public class MonitoredServiceServiceImpl implements MonitoredServiceService {
  @Inject private HealthSourceService healthSourceService;
  @Inject HPersistence hPersistence;

  @Override
  public MonitoredServiceResponse create(String accountId, MonitoredServiceDTO monitoredServiceDTO) {
    validate(monitoredServiceDTO);
    checkIfAlreadyPresent(accountId, monitoredServiceDTO);
    if (monitoredServiceDTO.getSources() != null) {
      healthSourceService.create(accountId, monitoredServiceDTO.getOrgIdentifier(),
          monitoredServiceDTO.getProjectIdentifier(), monitoredServiceDTO.getEnvironmentRef(),
          monitoredServiceDTO.getServiceRef(), monitoredServiceDTO.getIdentifier(),
          monitoredServiceDTO.getSources().getHealthSources());
    }
    saveMonitoredServiceEntity(accountId, monitoredServiceDTO);
    return get(accountId, monitoredServiceDTO.getOrgIdentifier(), monitoredServiceDTO.getProjectIdentifier(),
        monitoredServiceDTO.getIdentifier());
  }

  @Override
  public MonitoredServiceResponse update(String accountId, MonitoredServiceDTO monitoredServiceDTO) {
    MonitoredService monitoredService = getMonitoredService(accountId, monitoredServiceDTO.getOrgIdentifier(),
        monitoredServiceDTO.getProjectIdentifier(), monitoredServiceDTO.getIdentifier());
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
    updateMonitoredService(monitoredService, monitoredServiceDTO);
    return get(accountId, monitoredServiceDTO.getOrgIdentifier(), monitoredServiceDTO.getProjectIdentifier(),
        monitoredServiceDTO.getIdentifier());
  }

  private void updateMonitoredService(MonitoredService monitoredService, MonitoredServiceDTO monitoredServiceDTO) {
    UpdateOperations<MonitoredService> updateOperations = hPersistence.createUpdateOperations(MonitoredService.class);
    updateOperations.set(MonitoredServiceKeys.name, monitoredServiceDTO.getName());
    if (monitoredServiceDTO.getDescription() != null) {
      updateOperations.set(MonitoredServiceKeys.desc, monitoredServiceDTO.getDescription());
    }
    if (monitoredServiceDTO.getSources() != null) {
      List<String> updatedIdentifiers = monitoredServiceDTO.getSources()
                                            .getHealthSources()
                                            .stream()
                                            .map(healthSource -> healthSource.getIdentifier())
                                            .collect(Collectors.toList());
      updateOperations.set(MonitoredServiceKeys.healthSourceIdentifiers, updatedIdentifiers);
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
        monitoredService.getServiceIdentifier(), monitoredServiceDTO.getIdentifier(), toBeCreatedHealthSources);
    healthSourceService.update(monitoredService.getAccountId(), monitoredServiceDTO.getOrgIdentifier(),
        monitoredServiceDTO.getProjectIdentifier(), monitoredService.getEnvironmentIdentifier(),
        monitoredService.getServiceIdentifier(), monitoredServiceDTO.getIdentifier(), toBeUpdatedHealthSources);
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    MonitoredService monitoredService = getMonitoredService(accountId, orgIdentifier, projectIdentifier, identifier);
    if (monitoredService == null) {
      throw new InvalidRequestException(String.format(
          "Monitored Source Entity  with identifier %s and accountId %s is not present", identifier, accountId));
    }
    healthSourceService.delete(accountId, orgIdentifier, projectIdentifier, monitoredService.getIdentifier(),
        monitoredService.getHealthSourceIdentifiers());
    return hPersistence.delete(monitoredService);
  }

  @Override
  public MonitoredServiceResponse get(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    MonitoredService monitoredServiceEntity =
        getMonitoredService(accountId, orgIdentifier, projectIdentifier, identifier);
    if (monitoredServiceEntity == null) {
      throw new InvalidRequestException(
          String.format("Monitored Source Entity with identifier %s is not present", identifier));
    }
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
            .sources(
                Sources.builder()
                    .healthSources(healthSourceService.get(monitoredServiceEntity.getAccountId(),
                        monitoredServiceEntity.getOrgIdentifier(), monitoredServiceEntity.getProjectIdentifier(),
                        monitoredServiceEntity.getIdentifier(), monitoredServiceEntity.getHealthSourceIdentifiers()))
                    .build())
            .build();
    return MonitoredServiceResponse.builder()
        .monitoredService(monitoredServiceDTO)
        .createdAt(monitoredServiceEntity.getCreatedAt())
        .lastModifiedAt(monitoredServiceEntity.getLastUpdatedAt())
        .build();
  }

  @Override
  public MonitoredServiceResponse get(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String envIdentifier) {
    MonitoredService monitoredService =
        getMonitoredService(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier);
    if (monitoredService == null) {
      return null;
    }
    return get(accountId, orgIdentifier, projectIdentifier, monitoredService.getIdentifier());
  }

  @Override
  public MonitoredServiceDTO getMonitoredServiceDTO(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    return get(accountId, orgIdentifier, projectIdentifier, identifier).getMonitoredServiceDTO();
  }

  @Override
  public MonitoredServiceDTO getMonitoredServiceDTO(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String envIdentifier) {
    return get(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier).getMonitoredServiceDTO();
  }

  private MonitoredService getMonitoredService(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    return hPersistence.createQuery(MonitoredService.class)
        .filter(MonitoredServiceKeys.accountId, accountId)
        .filter(MonitoredServiceKeys.orgIdentifier, orgIdentifier)
        .filter(MonitoredServiceKeys.projectIdentifier, projectIdentifier)
        .filter(MonitoredServiceKeys.identifier, identifier)
        .get();
  }

  private MonitoredService getMonitoredService(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceRef, String envRef) {
    return hPersistence.createQuery(MonitoredService.class)
        .filter(MonitoredServiceKeys.accountId, accountId)
        .filter(MonitoredServiceKeys.orgIdentifier, orgIdentifier)
        .filter(MonitoredServiceKeys.projectIdentifier, projectIdentifier)
        .filter(MonitoredServiceKeys.serviceIdentifier, serviceRef)
        .filter(MonitoredServiceKeys.environmentIdentifier, envRef)
        .get();
  }

  private void checkIfAlreadyPresent(String accountId, MonitoredServiceDTO monitoredServiceDTO) {
    MonitoredService monitoredServiceEntity = getMonitoredService(accountId, monitoredServiceDTO.getOrgIdentifier(),
        monitoredServiceDTO.getProjectIdentifier(), monitoredServiceDTO.getIdentifier());
    if (monitoredServiceEntity != null) {
      throw new DuplicateFieldException(String.format(
          "Monitored Source Entity  with identifier %s and orgIdentifier %s and projectIdentifier %s is already present",
          monitoredServiceDTO.getIdentifier(), monitoredServiceDTO.getOrgIdentifier(),
          monitoredServiceDTO.getProjectIdentifier()));
    }
    monitoredServiceEntity =
        hPersistence.createQuery(MonitoredService.class)
            .filter(MonitoredServiceKeys.accountId, accountId)
            .filter(MonitoredServiceKeys.orgIdentifier, monitoredServiceDTO.getOrgIdentifier())
            .filter(MonitoredServiceKeys.projectIdentifier, monitoredServiceDTO.getProjectIdentifier())
            .filter(MonitoredServiceKeys.serviceIdentifier, monitoredServiceDTO.getServiceRef())
            .filter(MonitoredServiceKeys.environmentIdentifier, monitoredServiceDTO.getEnvironmentRef())
            .get();
    if (monitoredServiceEntity != null) {
      throw new DuplicateFieldException(String.format(
          "Monitored Source Entity  with duplicate service ref %s, environmentRef %s having identifier %s and orgIdentifier %s and projectIdentifier %s is already present",
          monitoredServiceDTO.getServiceRef(), monitoredServiceDTO.getEnvironmentRef(),
          monitoredServiceDTO.getIdentifier(), monitoredServiceDTO.getOrgIdentifier(),
          monitoredServiceDTO.getProjectIdentifier()));
    }
    if (monitoredServiceDTO.getSources() != null) {
      healthSourceService.checkIfAlreadyPresent(accountId, monitoredServiceDTO.getOrgIdentifier(),
          monitoredServiceDTO.getProjectIdentifier(), monitoredServiceDTO.getIdentifier(),
          monitoredServiceDTO.getSources().getHealthSources());
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
                                                  .build();
    if (monitoredServiceDTO.getSources() != null) {
      monitoredServiceEntity.setHealthSourceIdentifiers(monitoredServiceDTO.getSources()
                                                            .getHealthSources()
                                                            .stream()
                                                            .map(healthSourceInfo -> healthSourceInfo.getIdentifier())
                                                            .collect(Collectors.toList()));
    }
    hPersistence.save(monitoredServiceEntity);
  }

  @Override
  public PageResponse<MonitoredServiceListDTO> list(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentIdentifier, Integer offset, Integer pageSize, String filter) {
    List<MonitoredServiceListDTO> monitoredServiceListDTOS = new ArrayList<>();
    Query<MonitoredService> monitoredServicesQuery =
        hPersistence.createQuery(MonitoredService.class)
            .filter(MonitoredServiceKeys.accountId, accountId)
            .filter(MonitoredServiceKeys.orgIdentifier, orgIdentifier)
            .filter(MonitoredServiceKeys.projectIdentifier, projectIdentifier);
    if (environmentIdentifier != null) {
      monitoredServicesQuery.filter(MonitoredServiceKeys.environmentIdentifier, environmentIdentifier);
    }
    List<MonitoredService> monitoredServices = monitoredServicesQuery.asList();
    if (monitoredServices != null) {
      monitoredServiceListDTOS =
          monitoredServices.stream()
              .filter(monitoredService
                  -> isEmpty(filter) || monitoredService.getName().toLowerCase().contains(filter.trim().toLowerCase()))
              .map(monitoredService -> toMonitorServiceListDTO(monitoredService))
              .collect(Collectors.toList());
    }
    return PageUtils.offsetAndLimit(monitoredServiceListDTOS, offset, pageSize);
  }

  @Override
  public void deleteByProjectIdentifier(
      Class<MonitoredService> clazz, String accountId, String orgIdentifier, String projectIdentifier) {
    List<MonitoredService> monitoredServices = hPersistence.createQuery(MonitoredService.class)
                                                   .filter(MonitoredServiceKeys.accountId, accountId)
                                                   .filter(MonitoredServiceKeys.orgIdentifier, orgIdentifier)
                                                   .filter(MonitoredServiceKeys.projectIdentifier, projectIdentifier)
                                                   .asList();
    monitoredServices.forEach(
        monitoredService -> delete(accountId, orgIdentifier, projectIdentifier, monitoredService.getIdentifier()));
  }

  @Override
  public void deleteByOrgIdentifier(Class<MonitoredService> clazz, String accountId, String orgIdentifier) {
    List<MonitoredService> monitoredServices = hPersistence.createQuery(MonitoredService.class)
                                                   .filter(MonitoredServiceKeys.accountId, accountId)
                                                   .filter(MonitoredServiceKeys.orgIdentifier, orgIdentifier)
                                                   .asList();
    monitoredServices.forEach(monitoredService
        -> delete(accountId, orgIdentifier, monitoredService.getProjectIdentifier(), monitoredService.getIdentifier()));
  }

  @Override
  public void deleteByAccountIdentifier(Class<MonitoredService> clazz, String accountId) {
    List<MonitoredService> monitoredServices =
        hPersistence.createQuery(MonitoredService.class).filter(MonitoredServiceKeys.accountId, accountId).asList();
    monitoredServices.forEach(monitoredService
        -> delete(accountId, monitoredService.getOrgIdentifier(), monitoredService.getProjectIdentifier(),
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
      });
    }
  }

  @Override
  public List<String> listEnvironments(String accountId, String orgIdentifier, String projectIdentifier) {
    return hPersistence.createQuery(MonitoredService.class)
        .filter(MonitoredServiceKeys.accountId, accountId)
        .filter(MonitoredServiceKeys.orgIdentifier, orgIdentifier)
        .filter(MonitoredServiceKeys.projectIdentifier, projectIdentifier)
        .asList()
        .stream()
        .map(monitoredService -> monitoredService.getEnvironmentIdentifier())
        .collect(Collectors.toList());
  }

  @Override
  public MonitoredServiceResponse createDefault(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String environmentIdentifier) {
    MonitoredServiceDTO monitoredServiceDTO = MonitoredServiceDTO.builder()
                                                  .name(serviceIdentifier + "_" + environmentIdentifier)
                                                  .identifier(serviceIdentifier + "_" + environmentIdentifier)
                                                  .orgIdentifier(orgIdentifier)
                                                  .projectIdentifier(projectIdentifier)
                                                  .serviceRef(serviceIdentifier)
                                                  .environmentRef(environmentIdentifier)
                                                  .type(MonitoredServiceType.APPLICATION)
                                                  .description("Default Monitored Service")
                                                  .sources(Sources.builder().build())
                                                  .build();
    try {
      saveMonitoredServiceEntity(accountId, monitoredServiceDTO);
    } catch (DuplicateKeyException e) {
      monitoredServiceDTO.setIdentifier(monitoredServiceDTO.getIdentifier() + "_" + generateUuid().substring(0, 7));
      saveMonitoredServiceEntity(accountId, monitoredServiceDTO);
    }
    return get(accountId, orgIdentifier, projectIdentifier, monitoredServiceDTO.getIdentifier());
  }

  private MonitoredServiceListDTO toMonitorServiceListDTO(MonitoredService monitoredService) {
    return MonitoredServiceListDTO.builder()
        .name(monitoredService.getName())
        .identifier(monitoredService.getIdentifier())
        .serviceRef(monitoredService.getServiceIdentifier())
        .environmentRef(monitoredService.getEnvironmentIdentifier())
        .type(monitoredService.getType())
        .build();
  }
}
