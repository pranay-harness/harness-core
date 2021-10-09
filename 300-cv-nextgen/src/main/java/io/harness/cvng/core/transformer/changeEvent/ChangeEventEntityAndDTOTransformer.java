package io.harness.cvng.core.transformer.changeEvent;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;

import com.google.inject.Inject;
import java.util.Map;

public class ChangeEventEntityAndDTOTransformer {
  @Inject private Map<ChangeSourceType, ChangeEventMetaDataTransformer> changeTypeMetaDataTransformerMap;

  public Activity getEntity(ChangeEventDTO changeEventDTO) {
    ChangeEventMetaDataTransformer changeEventMetaDataTransformer =
        changeTypeMetaDataTransformerMap.get(changeEventDTO.getType());
    return changeEventMetaDataTransformer.getEntity(changeEventDTO);
  }

  public ChangeEventDTO getDto(Activity changeEvent) {
    ChangeEventMetaDataTransformer changeEventMetaDataTransformer =
        changeTypeMetaDataTransformerMap.get(ChangeSourceType.ofActivityType(changeEvent.getType()));
    return changeEventMetaDataTransformer.getDTO(changeEvent);
  }
}
