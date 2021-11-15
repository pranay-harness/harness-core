package io.harness.cvng.servicelevelobjective.services.impl;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.servicelevelobjective.beans.SLOTarget;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorSpec;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveFilter;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveResponse;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelObjective;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelObjective.ServiceLevelObjectiveKeys;
import io.harness.cvng.servicelevelobjective.services.ServiceLevelIndicatorService;
import io.harness.cvng.servicelevelobjective.services.ServiceLevelObjectiveService;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.persistence.HPersistence;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class ServiceLevelObjectiveServiceImpl implements ServiceLevelObjectiveService {
  @Inject private HPersistence hPersistence;

  @Inject private MonitoredServiceService monitoredServiceService;

  @Inject private ServiceLevelIndicatorService serviceLevelIndicatorService;

  @Override
  public ServiceLevelObjectiveResponse create(
      ProjectParams projectParams, ServiceLevelObjectiveDTO serviceLevelObjectiveDTO) {
    validate(serviceLevelObjectiveDTO, projectParams);
    saveServiceLevelObjectiveEntity(projectParams, serviceLevelObjectiveDTO);
    return getSLOResponse(serviceLevelObjectiveDTO.getIdentifier(), projectParams);
  }

  @Override
  public ServiceLevelObjectiveResponse update(
      ProjectParams projectParams, String identifier, ServiceLevelObjectiveDTO serviceLevelObjectiveDTO) {
    Preconditions.checkArgument(identifier.equals(serviceLevelObjectiveDTO.getIdentifier()),
        String.format("Identifier %s does not match with path identifier %s", serviceLevelObjectiveDTO.getIdentifier(),
            identifier));
    ServiceLevelObjective serviceLevelObjective =
        getServiceLevelObjective(projectParams, serviceLevelObjectiveDTO.getIdentifier());
    if (serviceLevelObjective == null) {
      throw new InvalidRequestException(String.format(
          "SLO  with identifier %s, accountId %s, orgIdentifier %s and projectIdentifier %s  is not present",
          serviceLevelObjectiveDTO.getIdentifier(), projectParams.getAccountIdentifier(),
          projectParams.getOrgIdentifier(), projectParams.getProjectIdentifier()));
    }
    validate(serviceLevelObjectiveDTO, projectParams);
    updateSLOEntity(projectParams, serviceLevelObjective, serviceLevelObjectiveDTO);
    return getSLOResponse(serviceLevelObjectiveDTO.getIdentifier(), projectParams);
  }

  @Override
  public boolean delete(ProjectParams projectParams, String identifier) {
    ServiceLevelObjective serviceLevelObjective = getServiceLevelObjective(projectParams, identifier);
    if (serviceLevelObjective == null) {
      throw new InvalidRequestException(String.format(
          "SLO  with identifier %s, accountId %s, orgIdentifier %s and projectIdentifier %s  is not present",
          identifier, projectParams.getAccountIdentifier(), projectParams.getOrgIdentifier(),
          projectParams.getProjectIdentifier()));
    }
    return hPersistence.delete(serviceLevelObjective);
  }

  @Override
  public PageResponse<ServiceLevelObjectiveResponse> get(ProjectParams projectParams, Integer offset, Integer pageSize,
      ServiceLevelObjectiveFilter serviceLevelObjectiveFilter) {
    Query<ServiceLevelObjective> sloQuery =
        hPersistence.createQuery(ServiceLevelObjective.class)
            .filter(ServiceLevelObjectiveKeys.accountId, projectParams.getAccountIdentifier())
            .filter(ServiceLevelObjectiveKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(ServiceLevelObjectiveKeys.projectIdentifier, projectParams.getProjectIdentifier())
            .order(Sort.descending(ServiceLevelObjectiveKeys.lastUpdatedAt));
    if (CollectionUtils.isNotEmpty(serviceLevelObjectiveFilter.getUserJourneys())) {
      sloQuery.field(ServiceLevelObjectiveKeys.userJourneyIdentifier).in(serviceLevelObjectiveFilter.getUserJourneys());
    }
    if (CollectionUtils.isNotEmpty(serviceLevelObjectiveFilter.getIdentifiers())) {
      sloQuery.field(ServiceLevelObjectiveKeys.identifier).in(serviceLevelObjectiveFilter.getIdentifiers());
    }
    List<ServiceLevelObjective> serviceLevelObjectiveList = sloQuery.asList();
    PageResponse<ServiceLevelObjective> sloEntitiesPageResponse =
        PageUtils.offsetAndLimit(serviceLevelObjectiveList, offset, pageSize);
    List<ServiceLevelObjectiveResponse> sloPageResponse =
        sloEntitiesPageResponse.getContent().stream().map(this::sloEntityToSLOResponse).collect(Collectors.toList());

    return PageResponse.<ServiceLevelObjectiveResponse>builder()
        .pageSize(pageSize)
        .pageIndex(offset)
        .totalPages(sloEntitiesPageResponse.getTotalPages())
        .totalItems(sloEntitiesPageResponse.getTotalItems())
        .pageItemCount(sloEntitiesPageResponse.getPageItemCount())
        .content(sloPageResponse)
        .build();
  }

  @Override
  public ServiceLevelObjectiveResponse get(ProjectParams projectParams, String identifier) {
    ServiceLevelObjective serviceLevelObjective = getServiceLevelObjective(projectParams, identifier);
    if (Objects.isNull(serviceLevelObjective)) {
      return null;
    }
    return sloEntityToSLOResponse(serviceLevelObjective);
  }

  private ServiceLevelObjective getServiceLevelObjective(ProjectParams projectParams, String identifier) {
    return hPersistence.createQuery(ServiceLevelObjective.class)
        .filter(ServiceLevelObjectiveKeys.accountId, projectParams.getAccountIdentifier())
        .filter(ServiceLevelObjectiveKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(ServiceLevelObjectiveKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .filter(ServiceLevelObjectiveKeys.identifier, identifier)
        .get();
  }

  private void updateSLOEntity(ProjectParams projectParams, ServiceLevelObjective serviceLevelObjective,
      ServiceLevelObjectiveDTO serviceLevelObjectiveDTO) {
    UpdateOperations<ServiceLevelObjective> updateOperations =
        hPersistence.createUpdateOperations(ServiceLevelObjective.class);
    updateOperations.set(ServiceLevelObjectiveKeys.name, serviceLevelObjectiveDTO.getName());
    if (serviceLevelObjectiveDTO.getDescription() != null) {
      updateOperations.set(ServiceLevelObjectiveKeys.desc, serviceLevelObjectiveDTO.getDescription());
    }
    updateOperations.set(ServiceLevelObjectiveKeys.tags, TagMapper.convertToList(serviceLevelObjectiveDTO.getTags()));
    updateOperations.set(ServiceLevelObjectiveKeys.userJourneyIdentifier, serviceLevelObjectiveDTO.getUserJourneyRef());
    updateOperations.set(
        ServiceLevelObjectiveKeys.monitoredServiceIdentifier, serviceLevelObjectiveDTO.getMonitoredServiceRef());
    updateOperations.set(
        ServiceLevelObjectiveKeys.healthSourceIdentifier, serviceLevelObjectiveDTO.getHealthSourceRef());
    updateOperations.set(ServiceLevelObjectiveKeys.serviceLevelIndicators,
        serviceLevelIndicatorService.update(projectParams, serviceLevelObjectiveDTO.getServiceLevelIndicators(),
            serviceLevelObjectiveDTO.getIdentifier(), serviceLevelObjective.getServiceLevelIndicators()));
    updateOperations.set(ServiceLevelObjectiveKeys.sloTarget, serviceLevelObjectiveDTO.getTarget());
    hPersistence.update(serviceLevelObjective, updateOperations);
  }

  private ServiceLevelObjectiveResponse getSLOResponse(String identifier, ProjectParams projectParams) {
    ServiceLevelObjective serviceLevelObjective = getServiceLevelObjective(projectParams, identifier);

    return sloEntityToSLOResponse(serviceLevelObjective);
  }

  private ServiceLevelObjectiveResponse sloEntityToSLOResponse(ServiceLevelObjective serviceLevelObjective) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(serviceLevelObjective.getAccountId())
                                      .orgIdentifier(serviceLevelObjective.getOrgIdentifier())
                                      .projectIdentifier(serviceLevelObjective.getProjectIdentifier())
                                      .build();
    ServiceLevelObjectiveDTO serviceLevelObjectiveDTO =
        ServiceLevelObjectiveDTO.builder()
            .identifier(serviceLevelObjective.getIdentifier())
            .name(serviceLevelObjective.getName())
            .description(serviceLevelObjective.getDesc())
            .monitoredServiceRef(serviceLevelObjective.getMonitoredServiceIdentifier())
            .healthSourceRef(serviceLevelObjective.getHealthSourceIdentifier())
            .serviceLevelIndicators(
                serviceLevelIndicatorService.get(projectParams, serviceLevelObjective.getServiceLevelIndicators()))
            .target(serviceLevelObjective.getSloTarget())
            .tags(TagMapper.convertToMap(serviceLevelObjective.getTags()))
            .userJourneyRef(serviceLevelObjective.getUserJourneyIdentifier())
            .build();
    return ServiceLevelObjectiveResponse.builder()
        .serviceLevelObjectiveDTO(serviceLevelObjectiveDTO)
        .createdAt(serviceLevelObjective.getCreatedAt())
        .lastModifiedAt(serviceLevelObjective.getLastUpdatedAt())
        .build();
  }

  private void saveServiceLevelObjectiveEntity(
      ProjectParams projectParams, ServiceLevelObjectiveDTO serviceLevelObjectiveDTO) {
    prePersistenceCleanup(serviceLevelObjectiveDTO);
    ServiceLevelObjective serviceLevelObjective =
        ServiceLevelObjective.builder()
            .accountId(projectParams.getAccountIdentifier())
            .orgIdentifier(projectParams.getOrgIdentifier())
            .projectIdentifier(projectParams.getProjectIdentifier())
            .identifier(serviceLevelObjectiveDTO.getIdentifier())
            .name(serviceLevelObjectiveDTO.getName())
            .desc(serviceLevelObjectiveDTO.getDescription())
            .serviceLevelIndicators(serviceLevelIndicatorService.create(projectParams,
                serviceLevelObjectiveDTO.getServiceLevelIndicators(), serviceLevelObjectiveDTO.getIdentifier()))
            .monitoredServiceIdentifier(serviceLevelObjectiveDTO.getMonitoredServiceRef())
            .healthSourceIdentifier(serviceLevelObjectiveDTO.getHealthSourceRef())
            .tags(TagMapper.convertToList(serviceLevelObjectiveDTO.getTags()))
            .sloTarget(serviceLevelObjectiveDTO.getTarget())
            .userJourneyIdentifier(serviceLevelObjectiveDTO.getUserJourneyRef())
            .build();

    hPersistence.save(serviceLevelObjective);
  }

  private void prePersistenceCleanup(ServiceLevelObjectiveDTO sloCreateDTO) {
    SLOTarget sloTarget = sloCreateDTO.getTarget();
    if (Objects.isNull(sloTarget.getType())) {
      sloTarget.setType(sloTarget.getSpec().getType());
    }
    for (ServiceLevelIndicatorDTO serviceLevelIndicator : sloCreateDTO.getServiceLevelIndicators()) {
      ServiceLevelIndicatorSpec serviceLevelIndicatorSpec = serviceLevelIndicator.getSpec();
      if (Objects.isNull(serviceLevelIndicatorSpec.getType())) {
        serviceLevelIndicatorSpec.setType(serviceLevelIndicatorSpec.getSpec().getType());
      }
    }
  }

  private void validate(ServiceLevelObjectiveDTO sloCreateDTO, ProjectParams projectParams) {
    monitoredServiceService.get(projectParams, sloCreateDTO.getMonitoredServiceRef());
  }
}
