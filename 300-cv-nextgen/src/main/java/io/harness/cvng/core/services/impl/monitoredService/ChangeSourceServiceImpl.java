package io.harness.cvng.core.services.impl.monitoredService;

import io.harness.cvng.core.beans.EnvironmentParams;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.entities.changeSource.ChangeSource;
import io.harness.cvng.core.entities.changeSource.ChangeSource.ChangeSourceKeys;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.transformer.changeSource.ChangeSourceEntityAndDTOTransformer;
import io.harness.cvng.core.types.ChangeSourceType;
import io.harness.exception.DuplicateFieldException;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
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
  @Inject private Map<ChangeSourceType, ChangeSource.UpdatableChangeSourceEntity> changeSourceUpdatableMap;

  @Override
  public void create(@NonNull EnvironmentParams environmentParams, @NonNull Set<ChangeSourceDTO> changeSourceDTOs) {
    if (CollectionUtils.isEmpty(changeSourceDTOs)) {
      return;
    }
    validate(changeSourceDTOs);
    validateChangeSourcesDoesntExist(environmentParams, changeSourceDTOs);
    List<ChangeSource> changeSources = changeSourceDTOs.stream()
                                           .map(dto -> changeSourceTransformer.getEntity(environmentParams, dto))
                                           .collect(Collectors.toList());
    hPersistence.save(changeSources);
  }

  @Override
  public Set<ChangeSourceDTO> get(@NonNull EnvironmentParams environmentParams, @NonNull List<String> identifiers) {
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
  public void delete(@NonNull EnvironmentParams environmentParams, @NonNull List<String> identifiers) {
    mongoQuery(environmentParams).field(ChangeSourceKeys.identifier).in(identifiers).forEach(hPersistence::delete);
  }

  @Override
  public void update(@NonNull EnvironmentParams environmentParams, @NonNull Set<ChangeSourceDTO> changeSourceDTOs) {
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
        .forEach(identifer -> update(newChangeSourceMap.get(identifer), existingChangeSourceMap.get(identifer)));

    hPersistence.save(newChangeSourceMap.keySet()
                          .stream()
                          .filter(key -> !replaceable(key, newChangeSourceMap, existingChangeSourceMap))
                          .map(newChangeSourceMap::get)
                          .collect(Collectors.toList()));

    hPersistence.delete(
        mongoQueryByUuids(existingChangeSourceMap.keySet()
                              .stream()
                              .filter(key -> !replaceable(key, newChangeSourceMap, existingChangeSourceMap))
                              .map(key -> existingChangeSourceMap.get(key).getUuid())
                              .collect(Collectors.toList())));
  }

  private void update(ChangeSource newChangeSource, ChangeSource existingChangeSource) {
    UpdateOperations<ChangeSource> updateOperations = hPersistence.createUpdateOperations(ChangeSource.class);
    changeSourceUpdatableMap.get(newChangeSource.getType()).setUpdateOperations(updateOperations, newChangeSource);
    hPersistence.update(
        hPersistence.createQuery(ChangeSource.class).filter(ChangeSourceKeys.uuid, existingChangeSource.getUuid()),
        updateOperations);
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

  private Query<ChangeSource> mongoQueryByUuids(List<String> uuids) {
    return hPersistence.createQuery(ChangeSource.class).field(ChangeSourceKeys.uuid).in(uuids);
  }

  private Query<ChangeSource> mongoQuery(EnvironmentParams environmentParams) {
    return hPersistence.createQuery(ChangeSource.class)
        .filter(ChangeSourceKeys.accountId, environmentParams.getProjectParams().getAccountIdentifier())
        .filter(ChangeSourceKeys.orgIdentifier, environmentParams.getProjectParams().getOrgIdentifier())
        .filter(ChangeSourceKeys.projectIdentifier, environmentParams.getProjectParams().getProjectIdentifier())
        .filter(ChangeSourceKeys.serviceIdentifier, environmentParams.getServiceIdentifier())
        .filter(ChangeSourceKeys.envIdentifier, environmentParams.getEnvIdentifier());
  }

  private void validateChangeSourcesDoesntExist(
      EnvironmentParams environmentParams, Set<ChangeSourceDTO> changeSourceDTOs) {
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
}
