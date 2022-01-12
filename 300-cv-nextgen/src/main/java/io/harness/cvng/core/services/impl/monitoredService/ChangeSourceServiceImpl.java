/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl.monitoredService;

import static io.harness.cvng.core.utils.FeatureFlagNames.CVNG_MONITORED_SERVICE_DEMO;

import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.cvng.beans.change.ChangeCategory;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.beans.change.HarnessCDCurrentGenEventMetadata;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.beans.change.ChangeSummaryDTO;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.ChangeSource;
import io.harness.cvng.core.entities.changeSource.ChangeSource.ChangeSourceKeys;
import io.harness.cvng.core.entities.changeSource.HarnessCDCurrentGenChangeSource;
import io.harness.cvng.core.entities.changeSource.KubernetesChangeSource;
import io.harness.cvng.core.services.api.ChangeEventService;
import io.harness.cvng.core.services.api.FeatureFlagService;
import io.harness.cvng.core.services.api.demo.ChangeSourceDemoDataGenerator;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.services.impl.ChangeSourceUpdateHandler;
import io.harness.cvng.core.transformer.changeSource.ChangeSourceEntityAndDTOTransformer;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class ChangeSourceServiceImpl implements ChangeSourceService {
  @Inject private HPersistence hPersistence;
  @Inject private ChangeSourceEntityAndDTOTransformer changeSourceTransformer;
  @Inject private ChangeEventService changeEventService;
  @Inject private Map<ChangeSourceType, ChangeSourceUpdateHandler> changeSourceUpdateHandlerMap;
  @Inject private Map<ChangeSourceType, ChangeSource.UpdatableChangeSourceEntity> changeSourceUpdatableMap;
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private Map<ChangeSourceType, ChangeSourceDemoDataGenerator> changeSourceTypeToDemoDataGeneratorMap;

  @Override
  public void create(
      @NonNull ServiceEnvironmentParams environmentParams, @NonNull Set<ChangeSourceDTO> changeSourceDTOs) {
    validate(changeSourceDTOs);
    validateChangeSourcesDoesntExist(environmentParams, changeSourceDTOs);
    List<ChangeSource> changeSources = changeSourceDTOs.stream()
                                           .map(dto -> changeSourceTransformer.getEntity(environmentParams, dto))
                                           .collect(Collectors.toList());
    create(changeSources);
  }
  private void create(List<ChangeSource> changeSources) {
    changeSources.forEach(changeSource -> setConfigForDemoIfApplicable(changeSource));

    hPersistence.save(changeSources);
    changeSources.stream()
        .filter(changeSource -> changeSourceUpdateHandlerMap.containsKey(changeSource.getType()))
        .forEach(changeSource -> changeSourceUpdateHandlerMap.get(changeSource.getType()).handleCreate(changeSource));
  }

  private void setConfigForDemoIfApplicable(ChangeSource changeSource) {
    if (changeSource.isEligibleForDemo()
        && featureFlagService.isFeatureFlagEnabled(changeSource.getAccountId(), CVNG_MONITORED_SERVICE_DEMO)) {
      changeSource.setConfiguredForDemo(true);
    }
  }

  @Override
  public Set<ChangeSourceDTO> get(
      @NonNull ServiceEnvironmentParams environmentParams, @NonNull List<String> identifiers) {
    if (CollectionUtils.isEmpty(identifiers)) {
      return Collections.emptySet();
    }
    return createQuery(environmentParams)
        .field(ChangeSourceKeys.identifier)
        .in(identifiers)
        .asList()
        .stream()
        .map(changeSourceTransformer::getDto)
        .collect(Collectors.toSet());
  }

  @Override
  public ChangeSource get(ServiceEnvironmentParams serviceEnvironmentParams, String identifier) {
    return createQuery(serviceEnvironmentParams).filter(ChangeSourceKeys.identifier, identifier).get();
  }

  @Override
  public Set<ChangeSourceDTO> getByType(ServiceEnvironmentParams environmentParams, ChangeSourceType changeSourceType) {
    return createQuery(environmentParams)
        .filter(ChangeSourceKeys.type, changeSourceType)
        .asList()
        .stream()
        .map(changeSourceTransformer::getDto)
        .collect(Collectors.toSet());
  }

  @Override
  public void delete(@NonNull ServiceEnvironmentParams environmentParams, @NonNull List<String> identifiers) {
    List<ChangeSource> changeSources =
        createQuery(environmentParams).field(ChangeSourceKeys.identifier).in(identifiers).asList();
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
        createQuery(environmentParams)
            .asList()
            .stream()
            .collect(Collectors.toMap(sc -> sc.getIdentifier(), Function.identity()));
    List<ChangeSource> changeSourcesToCreate = new ArrayList<>();
    newChangeSourceMap.forEach((identifier, changeSource) -> {
      if (replaceable(identifier, newChangeSourceMap, existingChangeSourceMap)) {
        ChangeSource existingChangeSource = existingChangeSourceMap.remove(identifier);
        update(existingChangeSource, changeSource);
        if (changeSourceUpdateHandlerMap.containsKey(changeSource.getType())) {
          changeSourceUpdateHandlerMap.get(changeSource.getType()).handleUpdate(existingChangeSource, changeSource);
        }
      } else {
        changeSourcesToCreate.add(changeSource);
      }
    });
    create(changeSourcesToCreate);
    existingChangeSourceMap.keySet().forEach(identifier -> {
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
            .projectIdentifier(changeSource.getProjectIdentifier())
            .orgIdentifier(changeSource.getOrgIdentifier())
            .envIdentifier(changeSource.getEnvIdentifier())
            .serviceIdentifier(changeSource.getServiceIdentifier())
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
      throw new InvalidRequestException(
          String.format("Multiple Change Sources exists with the same identifier %s", noUniqueIdentifier.get()));
    }
  }

  private Query<ChangeSource> createQuery(ServiceEnvironmentParams environmentParams) {
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
      throw new InvalidRequestException(String.format("Multiple Change Sources exists with the same identifier %s",
          changeSourceDTOS.iterator().next().getIdentifier()));
    }
  }

  private boolean replaceable(
      String key, Map<String, ChangeSource> newChangeSourceMap, Map<String, ChangeSource> existingChangeSourceMap) {
    return existingChangeSourceMap.containsKey(key) && newChangeSourceMap.containsKey(key)
        && existingChangeSourceMap.get(key).getClass().equals(newChangeSourceMap.get(key).getClass());
  }

  @Override
  public void handleCurrentGenEvents(HarnessCDCurrentGenChangeSource changeSource) {
    List<HarnessCDCurrentGenEventMetadata> events = verificationManagerService.getCurrentGenEvents(
        changeSource.getAccountId(), changeSource.getHarnessApplicationId(), changeSource.getHarnessEnvironmentId(),
        changeSource.getHarnessServiceId(), Instant.now().minus(5, ChronoUnit.MINUTES), Instant.now());
    events.forEach(event -> {
      ChangeEventDTO changeEventDTO = ChangeEventDTO.builder()
                                          .accountId(changeSource.getAccountId())
                                          .orgIdentifier(changeSource.getOrgIdentifier())
                                          .projectIdentifier(changeSource.getProjectIdentifier())
                                          .changeSourceIdentifier(changeSource.getIdentifier())
                                          .envIdentifier(changeSource.getEnvIdentifier())
                                          .serviceIdentifier(changeSource.getServiceIdentifier())
                                          .type(ChangeSourceType.HARNESS_CD_CURRENT_GEN)
                                          .eventTime(event.getWorkflowStartTime())
                                          .metadata(event)
                                          .build();
      changeEventService.register(changeEventDTO);
    });
  }
  @Override
  public void generateDemoData(ChangeSource entity) {
    if (changeSourceTypeToDemoDataGeneratorMap.containsKey(entity.getType())) {
      List<ChangeEventDTO> changeEvents = changeSourceTypeToDemoDataGeneratorMap.get(entity.getType()).generate(entity);
      changeEvents.forEach(changeEvent -> changeEventService.register(changeEvent));
    }
  }
}
