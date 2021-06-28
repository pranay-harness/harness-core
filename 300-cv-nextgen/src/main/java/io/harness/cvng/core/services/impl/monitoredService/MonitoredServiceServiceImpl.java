package io.harness.cvng.core.services.impl.monitoredService;

import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.Sources;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.entities.MonitoredService.MonitoredServiceKeys;
import io.harness.cvng.core.services.api.monitoredService.HealthSourceService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.mongodb.morphia.query.UpdateOperations;

public class MonitoredServiceServiceImpl implements MonitoredServiceService {
  @Inject private HealthSourceService healthSourceService;
  @Inject HPersistence hPersistence;

  @Override
  public void create(String accountId, MonitoredServiceDTO monitoredServiceDTO) {
    validate(monitoredServiceDTO);
    checkIfAlreadyPresent(accountId, monitoredServiceDTO);
    if (monitoredServiceDTO.getSources() != null) {
      healthSourceService.create(accountId, monitoredServiceDTO.getOrgIdentifier(),
          monitoredServiceDTO.getProjectIdentifier(), monitoredServiceDTO.getEnvironmentRef(),
          monitoredServiceDTO.getServiceRef(), monitoredServiceDTO.getSources().getHealthSources());
    }
    saveMonitoredServiceEntity(accountId, monitoredServiceDTO);
  }

  @Override
  public void update(String accountId, MonitoredServiceDTO monitoredServiceDTO) {
    MonitoredService monitoredService = getMonitoredService(accountId, monitoredServiceDTO.getOrgIdentifier(),
        monitoredServiceDTO.getProjectIdentifier(), monitoredServiceDTO.getIdentifier());
    if (monitoredService == null) {
      throw new InvalidRequestException(String.format(
          "Monitored Source Entity  with identifier %s, accountId %s, orgIdentifier %s and projectIdentifier %s  is not present",
          monitoredServiceDTO.getIdentifier(), accountId, monitoredServiceDTO.getOrgIdentifier(),
          monitoredService.getProjectIdentifier()));
    }
    Preconditions.checkArgument(monitoredService.getOrgIdentifier().equals(monitoredServiceDTO.getOrgIdentifier()),
        "orgIdentifier update is not allowed");
    Preconditions.checkArgument(
        monitoredService.getProjectIdentifier().equals(monitoredServiceDTO.getProjectIdentifier()),
        "projectIdentifier update is not allowed");
    Preconditions.checkArgument(monitoredService.getServiceIdentifier().equals(monitoredServiceDTO.getServiceRef()),
        "serviceRef update is not allowed");
    Preconditions.checkArgument(
        monitoredService.getEnvironmentIdentifier().equals(monitoredServiceDTO.getEnvironmentRef()),
        "environmentRef update is not allowed");
    validate(monitoredServiceDTO);
    updateHealthSources(monitoredService, monitoredServiceDTO);
    updateMonitoredService(monitoredService, monitoredServiceDTO);
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
        monitoredServiceDTO.getProjectIdentifier(), toBeDeletedIdentifiers);

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
        monitoredService.getServiceIdentifier(), toBeCreatedHealthSources);
    healthSourceService.update(monitoredService.getAccountId(), monitoredServiceDTO.getOrgIdentifier(),
        monitoredServiceDTO.getProjectIdentifier(), monitoredService.getEnvironmentIdentifier(),
        monitoredService.getServiceIdentifier(), toBeUpdatedHealthSources);
  }

  @Override
  public void delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    MonitoredService monitoredService = getMonitoredService(accountId, orgIdentifier, projectIdentifier, identifier);
    if (monitoredService == null) {
      throw new InvalidRequestException(String.format(
          "Monitored Source Entity  with identifier %s and accountId %s is not present", identifier, accountId));
    }
    healthSourceService.delete(
        accountId, orgIdentifier, projectIdentifier, monitoredService.getHealthSourceIdentifiers());
    hPersistence.delete(monitoredService);
  }

  @Override
  public MonitoredServiceDTO get(String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    MonitoredService monitoredServiceEntity =
        getMonitoredService(accountId, orgIdentifier, projectIdentifier, identifier);
    if (monitoredServiceEntity == null) {
      throw new InvalidRequestException(String.format(
          "Monitored Source Entity  with identifier %s and accountId %s is not present", identifier, accountId));
    }
    return MonitoredServiceDTO.builder()
        .name(monitoredServiceEntity.getName())
        .identifier(monitoredServiceEntity.getIdentifier())
        .orgIdentifier(monitoredServiceEntity.getOrgIdentifier())
        .projectIdentifier(monitoredServiceEntity.getProjectIdentifier())
        .environmentRef(monitoredServiceEntity.getEnvironmentIdentifier())
        .serviceRef(monitoredServiceEntity.getServiceIdentifier())
        .sources(Sources.builder()
                     .healthSources(healthSourceService.get(monitoredServiceEntity.getAccountId(),
                         monitoredServiceEntity.getOrgIdentifier(), monitoredServiceEntity.getProjectIdentifier(),
                         monitoredServiceEntity.getHealthSourceIdentifiers()))
                     .build())
        .build();
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

  private void checkIfAlreadyPresent(String accountId, MonitoredServiceDTO monitoredServiceDTO) {
    MonitoredService monitoredServiceEntity =
        hPersistence.createQuery(MonitoredService.class)
            .filter(MonitoredServiceKeys.accountId, accountId)
            .filter(MonitoredServiceKeys.orgIdentifier, monitoredServiceDTO.getOrgIdentifier())
            .filter(MonitoredServiceKeys.projectIdentifier, monitoredServiceDTO.getProjectIdentifier())
            .filter(MonitoredServiceKeys.identifier, monitoredServiceDTO.getIdentifier())
            .get();
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
          monitoredServiceDTO.getProjectIdentifier(), monitoredServiceDTO.getSources().getHealthSources());
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
}
