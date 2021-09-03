package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.beans.ChangeSummaryDTO;
import io.harness.cvng.core.beans.ChangeSummaryDTO.CategoryCountDetails;
import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.event.ChangeEvent;
import io.harness.cvng.core.entities.changeSource.event.ChangeEvent.ChangeEventKeys;
import io.harness.cvng.core.entities.changeSource.event.ChangeEvent.ChangeEventUpdatableEntity;
import io.harness.cvng.core.services.api.ChangeEventService;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.transformer.changeEvent.ChangeEventEntityAndDTOTransformer;
import io.harness.cvng.core.types.ChangeCategory;
import io.harness.cvng.core.types.ChangeSourceType;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

public class ChangeEventServiceImpl implements ChangeEventService {
  @Inject ChangeSourceService changeSourceService;
  @Inject ChangeEventEntityAndDTOTransformer transformer;
  @Inject private Map<ChangeSourceType, ChangeEventUpdatableEntity> eventMongoUtilMap;
  @Inject HPersistence hPersistence;

  @Override
  public Boolean register(String accountId, ChangeEventDTO changeEventDTO) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(accountId)
                                                            .orgIdentifier(changeEventDTO.getOrgIdentifier())
                                                            .projectIdentifier(changeEventDTO.getProjectIdentifier())
                                                            .serviceIdentifier(changeEventDTO.getServiceIdentifier())
                                                            .environmentIdentifier(changeEventDTO.getEnvIdentifier())
                                                            .build();
    Set<ChangeSourceDTO> changeSourceDTOS =
        changeSourceService.getByType(serviceEnvironmentParams, changeEventDTO.getType())
            .stream()
            .filter(source -> source.isEnabled())
            .collect(Collectors.toSet());
    if (changeSourceDTOS.isEmpty()) {
      return false;
    }
    upsert(changeEventDTO);
    return true;
  }

  @Override
  public List<ChangeEventDTO> get(ServiceEnvironmentParams serviceEnvironmentParams,
      List<String> changeSourceIdentifiers, Instant startTime, Instant endTime, List<ChangeCategory> changeCategories) {
    Query<ChangeEvent> query = mongoQuery(serviceEnvironmentParams, changeSourceIdentifiers, startTime, endTime);
    if (CollectionUtils.isNotEmpty(changeCategories)) {
      List<ChangeSourceType> changeSourceTypes =
          changeCategories.stream()
              .flatMap(changeCategory -> ChangeSourceType.getForCategory(changeCategory).stream())
              .collect(Collectors.toList());
      query = query.field(ChangeEventKeys.type).in(changeSourceTypes);
    }
    return query.asList().stream().map(changeEvent -> transformer.getDto(changeEvent)).collect(Collectors.toList());
  }

  @Override
  public ChangeSummaryDTO getChangeSummary(ServiceEnvironmentParams serviceEnvironmentParams,
      List<String> changeSourceIdentifiers, Instant startTime, Instant endTime) {
    return ChangeSummaryDTO.builder()
        .categoryCountMap(Arrays.stream(ChangeCategory.values())
                              .collect(Collectors.toMap(Function.identity(),
                                  changeCategory
                                  -> getCountDetails(serviceEnvironmentParams, changeSourceIdentifiers, startTime,
                                      endTime, changeCategory))))
        .build();
  }

  private CategoryCountDetails getCountDetails(ServiceEnvironmentParams serviceEnvironmentParams,
      List<String> changeSourceIdentifiers, Instant startTime, Instant endTime, ChangeCategory changeCategory) {
    Instant startTimeOfPreviousWindow = startTime.minus(Duration.between(startTime, endTime));
    return CategoryCountDetails.builder()
        .count(
            mongoQuery(serviceEnvironmentParams, changeSourceIdentifiers, startTime, endTime, changeCategory).count())
        .countInPrecedingWindow(mongoQuery(
            serviceEnvironmentParams, changeSourceIdentifiers, startTimeOfPreviousWindow, startTime, changeCategory)
                                    .count())
        .build();
  }

  private Query<ChangeEvent> mongoQuery(ServiceEnvironmentParams serviceEnvironmentParams,
      List<String> changeSourceIdentifiers, Instant startTime, Instant endTime, ChangeCategory changeCategory) {
    return mongoQuery(serviceEnvironmentParams, changeSourceIdentifiers, startTime, endTime)
        .field(ChangeEventKeys.type)
        .in(ChangeSourceType.getForCategory(changeCategory));
  }

  private Query<ChangeEvent> mongoQuery(ServiceEnvironmentParams serviceEnvironmentParams,
      List<String> changeSourceIdentifiers, Instant startTime, Instant endTime) {
    return mongoQuery(serviceEnvironmentParams)
        .field(ChangeEventKeys.changeSourceIdentifier)
        .in(changeSourceIdentifiers)
        .field(ChangeEventKeys.eventTime)
        .lessThan(endTime)
        .field(ChangeEventKeys.eventTime)
        .greaterThanOrEq(startTime);
  }

  private Query<ChangeEvent> mongoQuery(ServiceEnvironmentParams serviceEnvironmentParams) {
    return hPersistence.createQuery(ChangeEvent.class)
        .filter(ChangeEventKeys.accountId, serviceEnvironmentParams.getAccountIdentifier())
        .filter(ChangeEventKeys.orgIdentifier, serviceEnvironmentParams.getOrgIdentifier())
        .filter(ChangeEventKeys.projectIdentifier, serviceEnvironmentParams.getProjectIdentifier())
        .filter(ChangeEventKeys.envIdentifier, serviceEnvironmentParams.getEnvironmentIdentifier())
        .filter(ChangeEventKeys.serviceIdentifier, serviceEnvironmentParams.getServiceIdentifier());
  }

  private void upsert(ChangeEventDTO changeEventDTO) {
    ChangeEvent changeEvent = transformer.getEntity(changeEventDTO);
    ChangeEventUpdatableEntity changeEventUpdatableEntity = eventMongoUtilMap.get(changeEventDTO.getType());
    Optional<ChangeEvent> optionalFromDb = getFromDb(changeEvent, changeEventUpdatableEntity);
    if (optionalFromDb.isPresent()) {
      UpdateOperations<ChangeEvent> updateOperations =
          hPersistence.createUpdateOperations(changeEventUpdatableEntity.getEntityClass());
      changeEventUpdatableEntity.setUpdateOperations(updateOperations, changeEvent);
      hPersistence.update(optionalFromDb.get(), updateOperations);
    } else {
      hPersistence.save(changeEvent);
    }
  }

  private Optional<ChangeEvent> getFromDb(
      ChangeEvent changeEventDTO, ChangeEventUpdatableEntity changeEventUpdatableEntity) {
    return Optional.ofNullable(
        (ChangeEvent) eventMongoUtilMap.get(changeEventDTO.getType())
            .populateKeyQuery(hPersistence.createQuery(changeEventUpdatableEntity.getEntityClass()), changeEventDTO)
            .get());
  }
}
