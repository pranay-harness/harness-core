package io.harness.cvng.servicelevelobjective.services.impl;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorDTO;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator.ServiceLevelIndicatorKeys;
import io.harness.cvng.servicelevelobjective.services.ServiceLevelIndicatorService;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class ServiceLevelIndicatorServiceImpl implements ServiceLevelIndicatorService {
  @Inject private HPersistence hPersistence;

  @Override
  public List<String> create(ProjectParams projectParams, List<ServiceLevelIndicatorDTO> serviceLevelIndicatorDTOList,
      String serviceLevelObjectiveIdentifier) {
    List<String> serviceLevelIndicatorIdentifiers = new ArrayList<>();
    for (ServiceLevelIndicatorDTO serviceLevelIndicatorDTO : serviceLevelIndicatorDTOList) {
      if (Objects.isNull(serviceLevelIndicatorDTO.getName())
          && Objects.isNull(serviceLevelIndicatorDTO.getIdentifier())) {
        generateNameAndIdentifier(serviceLevelObjectiveIdentifier, serviceLevelIndicatorDTO);
      }
      saveServiceLevelIndicatorEntity(projectParams, serviceLevelIndicatorDTO);
      serviceLevelIndicatorIdentifiers.add(serviceLevelIndicatorDTO.getIdentifier());
    }
    return serviceLevelIndicatorIdentifiers;
  }

  private void generateNameAndIdentifier(
      String serviceLevelObjectiveIdentifier, ServiceLevelIndicatorDTO serviceLevelIndicatorDTO) {
    serviceLevelIndicatorDTO.setName(
        serviceLevelObjectiveIdentifier + "_" + serviceLevelIndicatorDTO.getSpec().getSpec().getMetricName());
    serviceLevelIndicatorDTO.setIdentifier(
        serviceLevelObjectiveIdentifier + "_" + serviceLevelIndicatorDTO.getSpec().getSpec().getMetricName());
  }

  @Override
  public List<ServiceLevelIndicatorDTO> get(ProjectParams projectParams, List<String> serviceLevelIndicators) {
    List<ServiceLevelIndicator> serviceLevelIndicatorList =
        hPersistence.createQuery(ServiceLevelIndicator.class)
            .filter(ServiceLevelIndicatorKeys.accountId, projectParams.getAccountIdentifier())
            .filter(ServiceLevelIndicatorKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(ServiceLevelIndicatorKeys.projectIdentifier, projectParams.getProjectIdentifier())
            .field(ServiceLevelIndicatorKeys.identifier)
            .in(serviceLevelIndicators)
            .asList();
    return serviceLevelIndicatorList.stream().map(this::sliEntityToDTO).collect(Collectors.toList());
  }

  @Override
  public List<String> update(ProjectParams projectParams, List<ServiceLevelIndicatorDTO> serviceLevelIndicatorDTOList,
      String serviceLevelObjectiveIdentifier, List<String> serviceLevelIndicatorsList) {
    List<String> serviceLevelIndicatorIdentifiers = new ArrayList<>();
    for (ServiceLevelIndicatorDTO serviceLevelIndicatorDTO : serviceLevelIndicatorDTOList) {
      if (Objects.isNull(serviceLevelIndicatorDTO.getName())
          && Objects.isNull(serviceLevelIndicatorDTO.getIdentifier())) {
        generateNameAndIdentifier(serviceLevelObjectiveIdentifier, serviceLevelIndicatorDTO);
      }
      ServiceLevelIndicator serviceLevelIndicator =
          getServiceLevelIndicator(projectParams, serviceLevelIndicatorDTO.getIdentifier());
      if (Objects.isNull(serviceLevelIndicator)) {
        saveServiceLevelIndicatorEntity(projectParams, serviceLevelIndicatorDTO);
      } else {
        updateServiceLevelIndicatorEntity(projectParams, serviceLevelIndicatorDTO);
      }
      serviceLevelIndicatorIdentifiers.add(serviceLevelIndicatorDTO.getIdentifier());
    }
    List<String> toBeDeletedIdentifiers =
        serviceLevelIndicatorsList.stream()
            .filter(identifier -> !serviceLevelIndicatorIdentifiers.contains(identifier))
            .collect(Collectors.toList());
    deleteByIdentifier(projectParams, toBeDeletedIdentifiers);
    return serviceLevelIndicatorIdentifiers;
  }

  @Override
  public void deleteByIdentifier(ProjectParams projectParams, List<String> serviceLevelIndicatorIdentifier) {
    if (CollectionUtils.isNotEmpty(serviceLevelIndicatorIdentifier)) {
      hPersistence.delete(hPersistence.createQuery(ServiceLevelIndicator.class)
                              .filter(ServiceLevelIndicatorKeys.accountId, projectParams.getAccountIdentifier())
                              .filter(ServiceLevelIndicatorKeys.orgIdentifier, projectParams.getOrgIdentifier())
                              .filter(ServiceLevelIndicatorKeys.projectIdentifier, projectParams.getProjectIdentifier())
                              .field(ServiceLevelIndicatorKeys.identifier)
                              .in(serviceLevelIndicatorIdentifier));
    }
  }

  private void updateServiceLevelIndicatorEntity(
      ProjectParams projectParams, ServiceLevelIndicatorDTO serviceLevelIndicatorDTO) {
    ServiceLevelIndicator serviceLevelIndicator =
        getServiceLevelIndicator(projectParams, serviceLevelIndicatorDTO.getIdentifier());
    UpdateOperations<ServiceLevelIndicator> updateOperations =
        hPersistence.createUpdateOperations(ServiceLevelIndicator.class);
    updateOperations.set(ServiceLevelIndicatorKeys.type, serviceLevelIndicatorDTO.getType());
    updateOperations.set(ServiceLevelIndicatorKeys.spec, serviceLevelIndicatorDTO.getSpec());
    hPersistence.update(serviceLevelIndicator, updateOperations);
  }

  private void saveServiceLevelIndicatorEntity(
      ProjectParams projectParams, ServiceLevelIndicatorDTO serviceLevelIndicatorDTO) {
    ServiceLevelIndicator serviceLevelIndicator = ServiceLevelIndicator.builder()
                                                      .accountId(projectParams.getAccountIdentifier())
                                                      .orgIdentifier(projectParams.getOrgIdentifier())
                                                      .projectIdentifier(projectParams.getProjectIdentifier())
                                                      .spec(serviceLevelIndicatorDTO.getSpec())
                                                      .name(serviceLevelIndicatorDTO.getName())
                                                      .identifier(serviceLevelIndicatorDTO.getIdentifier())
                                                      .type(serviceLevelIndicatorDTO.getType())
                                                      .build();

    hPersistence.save(serviceLevelIndicator);
  }

  private ServiceLevelIndicator getServiceLevelIndicator(ProjectParams projectParams, String identifier) {
    return hPersistence.createQuery(ServiceLevelIndicator.class)
        .filter(ServiceLevelIndicatorKeys.accountId, projectParams.getAccountIdentifier())
        .filter(ServiceLevelIndicatorKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(ServiceLevelIndicatorKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .filter(ServiceLevelIndicatorKeys.identifier, identifier)
        .get();
  }

  private ServiceLevelIndicatorDTO sliEntityToDTO(ServiceLevelIndicator serviceLevelIndicator) {
    return ServiceLevelIndicatorDTO.builder()
        .identifier(serviceLevelIndicator.getIdentifier())
        .name(serviceLevelIndicator.getName())
        .type(serviceLevelIndicator.getType())
        .spec(serviceLevelIndicator.getSpec())
        .build();
  }
}
