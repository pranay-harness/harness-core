package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.event.ChangeEvent;
import io.harness.cvng.core.entities.changeSource.event.ChangeEvent.ChangeEventUpdatableEntity;
import io.harness.cvng.core.services.api.ChangeEventService;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.transformer.changeEvent.ChangeEventEntityAndDTOTransformer;
import io.harness.cvng.core.types.ChangeSourceType;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
