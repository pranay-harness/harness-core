package io.harness.cvng.core.services.impl.monitoredService;

import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.Sources;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.entities.MonitoredService.MonitoredServiceKeys;
import io.harness.cvng.core.services.api.monitoredService.HealthSourceService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.stream.Collectors;

public class MonitoredServiceServiceImpl implements MonitoredServiceService {
  @Inject private HealthSourceService healthSourceService;
  @Inject HPersistence hPersistence;

  @Override
  public void create(String accountId, MonitoredServiceDTO monitoredServiceDTO) {
    checkIfAlreadyPresent(accountId, monitoredServiceDTO);
    if (monitoredServiceDTO.getSources() != null) {
      healthSourceService.create(accountId, monitoredServiceDTO.getOrgIdentifier(),
          monitoredServiceDTO.getProjectIdentifier(), monitoredServiceDTO.getEnvironmentRef(),
          monitoredServiceDTO.getServiceRef(), monitoredServiceDTO.getSources().getHealthSources());
    }
    saveMonitoredServiceEntity(accountId, monitoredServiceDTO);
  }

  @Override
  public MonitoredServiceDTO get(String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    MonitoredService monitoredServiceEntity = hPersistence.createQuery(MonitoredService.class)
                                                  .filter(MonitoredServiceKeys.accountId, accountId)
                                                  .filter(MonitoredServiceKeys.orgIdentifier, orgIdentifier)
                                                  .filter(MonitoredServiceKeys.projectIdentifier, projectIdentifier)
                                                  .filter(MonitoredServiceKeys.identifier, identifier)
                                                  .get();
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

  // TODO: implement delete logic based on these identifiers(part of next pr along with delete api)
  @Override
  public void deleteByProjectIdentifier(
      Class<MonitoredService> clazz, String accountId, String orgIdentifier, String projectIdentifier) {}

  @Override
  public void deleteByOrgIdentifier(Class<MonitoredService> clazz, String accountId, String orgIdentifier) {}

  @Override
  public void deleteByAccountIdentifier(Class<MonitoredService> clazz, String accountId) {}
}
