package io.harness.cvng.core.services.impl.monitoredService;

import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.cvng.beans.change.ChangeCategory;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.beans.ChangeSummaryDTO;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.ChangeSource;
import io.harness.cvng.core.entities.changeSource.ChangeSource.ChangeSourceKeys;
import io.harness.cvng.core.entities.changeSource.KubernetesChangeSource;
import io.harness.cvng.core.services.api.ChangeEventService;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.services.impl.ChangeSourceUpdateHandler;
import io.harness.cvng.core.transformer.changeSource.ChangeSourceEntityAndDTOTransformer;
import io.harness.exception.DuplicateFieldException;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

public class ChangeSourceServiceImpl implements ChangeSourceService {
  @Inject private HPersistence hPersistence;
  @Inject private ChangeSourceEntityAndDTOTransformer changeSourceTransformer;
  @Inject private ChangeEventService changeEventService;
  @Inject private Map<ChangeSourceType, ChangeSourceUpdateHandler> changeSourceUpdateHandlerMap;
  @Inject private Map<ChangeSourceType, ChangeSource.UpdatableChangeSourceEntity> changeSourceUpdatableMap;
  @Inject private VerificationManagerService verificationManagerService;

  @Override
  public void create(
      @NonNull ServiceEnvironmentParams environmentParams, @NonNull Set<ChangeSourceDTO> changeSourceDTOs) {
    validate(changeSourceDTOs);
    validateChangeSourcesDoesntExist(environmentParams, changeSourceDTOs);
    List<ChangeSource> changeSources = changeSourceDTOs.stream()
                                           .map(dto -> changeSourceTransformer.getEntity(environmentParams, dto))
                                           .collect(Collectors.toList());
    hPersistence.save(changeSources);
    changeSources.stream()
        .filter(changeSource -> changeSourceUpdateHandlerMap.containsKey(changeSource.getType()))
        .forEach(changeSource -> changeSourceUpdateHandlerMap.get(changeSource.getType()).handleCreate(changeSource));
  }

  @Override
  public Set<ChangeSourceDTO> get(
      @NonNull ServiceEnvironmentParams environmentParams, @NonNull List<String> identifiers) {
    if (CollectionUtils.isEmpty(identifiers)) {
      return Collections.emptySet();
    }
    return mongoQuery(environmentParams)
        .field(ChangeSourceKeys.identifier)
        .in(identifiers)
        .asList()
        .stream()
        .map(changeSourceTransformer::getDto)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<ChangeSourceDTO> getByType(ServiceEnvironmentParams environmentParams, ChangeSourceType changeSourceType) {
    return mongoQuery(environmentParams)
        .filter(ChangeSourceKeys.type, changeSourceType)
        .asList()
        .stream()
        .map(changeSourceTransformer::getDto)
        .collect(Collectors.toSet());
  }

  @Override
  public void delete(@NonNull ServiceEnvironmentParams environmentParams, @NonNull List<String> identifiers) {
    List<ChangeSource> changeSources =
        mongoQuery(environmentParams).field(ChangeSourceKeys.identifier).in(identifiers).asList();
    changeSources.forEach(changeSource -> {
      hPersistence.delete(changeSource);
      if (changeSourceUpdateHandlerMap.containsKey(changeSource.getType())) {
        changeSourceUpdateHandlerMap.get(changeSource.getType()).handleDelete(changeSource);
      }
    });
  }

  @Override
  public void update(
      @NonNull ServiceEnvironmentParams environmentParams, @NonNull Set<ChangeSourceDTO> changeSourceDTOs) {
    validate(changeSourceDTOs);
    Map<String, ChangeSource> newChangeSourceMap =
        changeSourceDTOs.stream()
            .map(dto -> changeSourceTransformer.getEntity(environmentParams, dto))
            .collect(Collectors.toMap(cs -> cs.getIdentifier(), Function.identity()));

    Map<String, ChangeSource> existingChangeSourceMap =
        mongoQuery(environmentParams)
            .asList()
            .stream()
            .collect(Collectors.toMap(sc -> sc.getIdentifier(), Function.identity()));

    newChangeSourceMap.keySet()
        .stream()
        .filter(key -> replaceable(key, newChangeSourceMap, existingChangeSourceMap))
        .forEach(identifer -> {
          ChangeSource existingChangeSource = existingChangeSourceMap.get(identifer);
          ChangeSource newChangeSource = newChangeSourceMap.get(identifer);
          update(existingChangeSource, newChangeSource);
          if (changeSourceUpdateHandlerMap.containsKey(newChangeSource.getType())) {
            changeSourceUpdateHandlerMap.get(newChangeSource.getType())
                .handleUpdate(existingChangeSource, newChangeSource);
          }
        });

    newChangeSourceMap.keySet()
        .stream()
        .filter(key -> !replaceable(key, newChangeSourceMap, existingChangeSourceMap))
        .forEach(identifier -> {
          ChangeSource changeSource = newChangeSourceMap.get(identifier);
          hPersistence.save(changeSource);
          if (changeSourceUpdateHandlerMap.containsKey(changeSource.getType())) {
            changeSourceUpdateHandlerMap.get(changeSource.getType()).handleCreate(changeSource);
          }
        });

    existingChangeSourceMap.keySet()
        .stream()
        .filter(key -> !replaceable(key, newChangeSourceMap, existingChangeSourceMap))
        .forEach(identifier -> {
          ChangeSource changeSource = existingChangeSourceMap.get(identifier);
          hPersistence.delete(changeSource);
          if (changeSourceUpdateHandlerMap.containsKey(changeSource.getType())) {
            changeSourceUpdateHandlerMap.get(changeSource.getType()).handleDelete(changeSource);
          }
        });
  }

  protected void update(ChangeSource existingChangeSource, ChangeSource newChangeSource) {
    UpdateOperations<ChangeSource> updateOperations = hPersistence.createUpdateOperations(ChangeSource.class);
    changeSourceUpdatableMap.get(newChangeSource.getType()).setUpdateOperations(updateOperations, newChangeSource);
    hPersistence.update(
        hPersistence.createQuery(ChangeSource.class).filter(ChangeSourceKeys.uuid, existingChangeSource.getUuid()),
        updateOperations);
  }

  @Override
  public void enqueueDataCollectionTask(KubernetesChangeSource changeSource) {
    DataCollectionConnectorBundle dataCollectionConnectorBundle =
        DataCollectionConnectorBundle.builder()
            .dataCollectionType(DataCollectionType.KUBERNETES)
            .connectorIdentifier(changeSource.getConnectorIdentifier())
            .sourceIdentifier(changeSource.getIdentifier())
            .dataCollectionWorkerId(changeSource.getUuid())
            .build();

    String dataCollectionTaskId = verificationManagerService.createDataCollectionTask(changeSource.getAccountId(),
        changeSource.getOrgIdentifier(), changeSource.getProjectIdentifier(), dataCollectionConnectorBundle);

    UpdateOperations<ChangeSource> updateOperations =
        hPersistence.createUpdateOperations(ChangeSource.class)
            .set(ChangeSourceKeys.dataCollectionTaskId, dataCollectionTaskId);
    Query<ChangeSource> query =
        hPersistence.createQuery(ChangeSource.class).filter(ChangeSourceKeys.uuid, changeSource.getUuid());
    hPersistence.update(query, updateOperations);
  }

  @Override
  public List<ChangeEventDTO> getChangeEvents(ServiceEnvironmentParams serviceEnvironmentParams,
      List<String> changeSourceIdentifiers, Instant startTime, Instant endTime, List<ChangeCategory> changeCategories) {
    return changeEventService.get(
        serviceEnvironmentParams, changeSourceIdentifiers, startTime, endTime, changeCategories);
  }

  @Override
  public ChangeSummaryDTO getChangeSummary(ServiceEnvironmentParams serviceEnvironmentParams,
      List<String> changeSourceIdentifiers, Instant startTime, Instant endTime) {
    return changeEventService.getChangeSummary(serviceEnvironmentParams, changeSourceIdentifiers, startTime, endTime);
  }

  private void validate(Set<ChangeSourceDTO> changeSourceDTOs) {
    Optional<String> noUniqueIdentifier =
        changeSourceDTOs.stream()
            .map(dto -> dto.getIdentifier())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet()
            .stream()
            .filter(element -> element.getValue() > 1)
            .map(entrySet -> entrySet.getKey())
            .findAny();
    if (noUniqueIdentifier.isPresent()) {
      throw new DuplicateFieldException(Pair.of(ChangeSourceKeys.identifier, noUniqueIdentifier.get()));
    }
  }

  private Query<ChangeSource> mongoQuery(ServiceEnvironmentParams environmentParams) {
    return hPersistence.createQuery(ChangeSource.class)
        .filter(ChangeSourceKeys.accountId, environmentParams.getAccountIdentifier())
        .filter(ChangeSourceKeys.orgIdentifier, environmentParams.getOrgIdentifier())
        .filter(ChangeSourceKeys.projectIdentifier, environmentParams.getProjectIdentifier())
        .filter(ChangeSourceKeys.serviceIdentifier, environmentParams.getServiceIdentifier())
        .filter(ChangeSourceKeys.envIdentifier, environmentParams.getEnvironmentIdentifier());
  }

  private void validateChangeSourcesDoesntExist(
      ServiceEnvironmentParams environmentParams, Set<ChangeSourceDTO> changeSourceDTOs) {
    Set<ChangeSourceDTO> changeSourceDTOS = get(environmentParams,
        changeSourceDTOs.stream().map(changeSourceDTO -> changeSourceDTO.getIdentifier()).collect(Collectors.toList()));

    if (CollectionUtils.isNotEmpty(changeSourceDTOS)) {
      throw new DuplicateFieldException(
          Pair.of(ChangeSourceKeys.identifier, changeSourceDTOS.iterator().next().getIdentifier()));
    }
  }

  private boolean replaceable(
      String key, Map<String, ChangeSource> newChangeSourceMap, Map<String, ChangeSource> existingChangeSourceMap) {
    return existingChangeSourceMap.containsKey(key) && newChangeSourceMap.containsKey(key)
        && existingChangeSourceMap.get(key).getClass().equals(newChangeSourceMap.get(key).getClass());
  }

  @Override
  public void deleteByProjectIdentifier(
      Class<ChangeSource> clazz, String accountId, String orgIdentifier, String projectIdentifier) {
    hPersistence.createQuery(ChangeSource.class)
        .filter(ChangeSourceKeys.accountId, accountId)
        .filter(ChangeSourceKeys.orgIdentifier, orgIdentifier)
        .filter(ChangeSourceKeys.projectIdentifier, projectIdentifier)
        .forEach(hPersistence::delete);
  }

  @Override
  public void deleteByOrgIdentifier(Class<ChangeSource> clazz, String accountId, String orgIdentifier) {
    hPersistence.createQuery(ChangeSource.class)
        .filter(ChangeSourceKeys.accountId, accountId)
        .filter(ChangeSourceKeys.orgIdentifier, orgIdentifier)
        .forEach(hPersistence::delete);
  }

  @Override
  public void deleteByAccountIdentifier(Class<ChangeSource> clazz, String accountId) {
    hPersistence.createQuery(ChangeSource.class)
        .filter(ChangeSourceKeys.accountId, accountId)
        .forEach(hPersistence::delete);
  }
}
