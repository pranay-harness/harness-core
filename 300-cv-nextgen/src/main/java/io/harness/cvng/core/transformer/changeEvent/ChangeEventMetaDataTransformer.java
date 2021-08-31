package io.harness.cvng.core.transformer.changeEvent;

import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.change.event.metadata.ChangeEventMetaData;
import io.harness.cvng.core.entities.changeSource.event.ChangeEvent;

public abstract class ChangeEventMetaDataTransformer<E extends ChangeEvent, M extends ChangeEventMetaData> {
  public abstract E getEntity(ChangeEventDTO changeEventDTO);

  public final ChangeEventDTO getDTO(E changeEvent) {
    return ChangeEventDTO.builder()
        .accountId(changeEvent.getAccountId())
        .orgIdentifier(changeEvent.getOrgIdentifier())
        .projectIdentifier(changeEvent.getProjectIdentifier())
        .serviceIdentifier(changeEvent.getServiceIdentifier())
        .envIdentifier(changeEvent.getEnvIdentifier())
        .eventTime(changeEvent.getEventTime())
        .type(changeEvent.getType())
        .changeEventMetaData(getMetadata(changeEvent))
        .build();
  }

  protected abstract M getMetadata(E changeEvent);
}
