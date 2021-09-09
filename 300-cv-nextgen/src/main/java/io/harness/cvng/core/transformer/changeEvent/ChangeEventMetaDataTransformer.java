package io.harness.cvng.core.transformer.changeEvent;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.change.event.metadata.ChangeEventMetaData;
import io.harness.cvng.core.types.ChangeSourceType;

public abstract class ChangeEventMetaDataTransformer<E extends Activity, M extends ChangeEventMetaData> {
  public abstract E getEntity(ChangeEventDTO changeEventDTO);

  public final ChangeEventDTO getDTO(E activity) {
    return ChangeEventDTO.builder()
        .accountId(activity.getAccountId())
        .orgIdentifier(activity.getOrgIdentifier())
        .projectIdentifier(activity.getProjectIdentifier())
        .serviceIdentifier(activity.getServiceIdentifier())
        .changeSourceIdentifier(activity.getChangeSourceIdentifier())
        .envIdentifier(activity.getEnvironmentIdentifier())
        .eventTime(activity.getEventTime().toEpochMilli())
        .type(ChangeSourceType.ofActivityType(activity.getType()))
        .changeEventMetaData(getMetadata(activity))
        .build();
  }

  protected abstract M getMetadata(E activity);
}
