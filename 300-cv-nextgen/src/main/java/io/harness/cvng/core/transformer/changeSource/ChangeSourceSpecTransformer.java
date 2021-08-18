package io.harness.cvng.core.transformer.changeSource;

import io.harness.cvng.core.beans.EnvironmentParams;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.monitoredService.changeSourceSpec.ChangeSourceSpec;
import io.harness.cvng.core.entities.changeSource.ChangeSource;

public abstract class ChangeSourceSpecTransformer<E extends ChangeSource, S extends ChangeSourceSpec> {
  public abstract E getEntity(EnvironmentParams environmentParams, ChangeSourceDTO changeSourceDTO);

  public final ChangeSourceDTO getDTO(E changeSource) {
    return ChangeSourceDTO.builder()
        .spec(getSpec(changeSource))
        .description(changeSource.getDescription())
        .enabled(changeSource.isEnabled())
        .identifier(changeSource.getIdentifier())
        .type(changeSource.getType())
        .build();
  }

  protected abstract S getSpec(E changeSource);
}
