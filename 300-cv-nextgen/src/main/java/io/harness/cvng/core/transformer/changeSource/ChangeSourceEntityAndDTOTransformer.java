package io.harness.cvng.core.transformer.changeSource;

import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.ChangeSource;

import com.google.inject.Inject;
import java.util.Map;

public class ChangeSourceEntityAndDTOTransformer {
  @Inject private Map<ChangeSourceType, ChangeSourceSpecTransformer> changeSourceTypeChangeSourceSpecTransformerMap;

  public ChangeSource getEntity(ServiceEnvironmentParams environmentParams, ChangeSourceDTO changeSourceDTO) {
    ChangeSourceSpecTransformer changeSourceSpecTransformer =
        changeSourceTypeChangeSourceSpecTransformerMap.get(changeSourceDTO.getSpec().getType());
    return changeSourceSpecTransformer.getEntity(environmentParams, changeSourceDTO);
  }

  public ChangeSourceDTO getDto(ChangeSource changeSource) {
    ChangeSourceSpecTransformer changeSourceSpecTransformer =
        changeSourceTypeChangeSourceSpecTransformerMap.get(changeSource.getType());
    return changeSourceSpecTransformer.getDTO(changeSource);
  }
}
