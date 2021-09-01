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
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.UpdateOperations;

public class ChangeEventServiceImpl implements ChangeEventService {
  @Inject ChangeSourceService changeSourceService;
  @Inject ChangeEventEntityAndDTOTransformer transformer;
  @Inject private Map<ChangeSourceType, ChangeEventUpdatableEntity> changeEventUpdatableEntityMap;
  @Inject HPersistence hPersistence;

  @Override
  public Boolean register(ChangeEventDTO changeEventDTO) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(changeEventDTO.getAccountId())
                                                            .orgIdentifier(changeEventDTO.getOrgIdentifier())
                                                            .projectIdentifier(changeEventDTO.getProjectIdentifier())
                                                            .serviceIdentifier(changeEventDTO.getServiceIdentifier())
                                                            .environmentIdentifier(changeEventDTO.getEnvIdentifier())
                                                            .build();
    Optional<ChangeSourceDTO> changeSourceDTOOptional =
        changeSourceService.getByType(serviceEnvironmentParams, changeEventDTO.getType())
            .stream()
            .filter(source -> source.isEnabled())
            .findAny();
    if (!changeSourceDTOOptional.isPresent()) {
      return false;
    }
    if (StringUtils.isEmpty(changeEventDTO.getChangeSourceIdentifier())) {
      changeEventDTO.setChangeSourceIdentifier(changeSourceDTOOptional.get().getIdentifier());
    }
    changeEventDTO.setChangeSourceIdentifier(changeSourceDTOOptional.get().getIdentifier());
    upsert(changeEventDTO);
    return true;
  }

  private void upsert(ChangeEventDTO changeEventDTO) {
    ChangeEvent changeEvent = transformer.getEntity(changeEventDTO);
    ChangeEventUpdatableEntity changeEventUpdatableEntity = changeEventUpdatableEntityMap.get(changeEventDTO.getType());
    Optional<ChangeEvent> optionalFromDb = getFromDb(changeEvent);
    if (optionalFromDb.isPresent()) {
      UpdateOperations<ChangeEvent> updateOperations =
          hPersistence.createUpdateOperations(changeEventUpdatableEntity.getEntityClass());
      changeEventUpdatableEntity.setUpdateOperations(updateOperations, changeEvent);
      hPersistence.update(optionalFromDb.get(), updateOperations);
    } else {
      hPersistence.save(changeEvent);
    }
  }

  private Optional<ChangeEvent> getFromDb(ChangeEvent changeEventDTO) {
    ChangeEventUpdatableEntity changeEventUpdatableEntity = changeEventUpdatableEntityMap.get(changeEventDTO.getType());
    return Optional.ofNullable(
        (ChangeEvent) changeEventUpdatableEntity
            .populateKeyQuery(hPersistence.createQuery(changeEventUpdatableEntity.getEntityClass()), changeEventDTO)
            .get());
  }
}
