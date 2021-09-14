/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.transformer.changeSource;

import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.monitoredService.changeSourceSpec.ChangeSourceSpec;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.ChangeSource;

public abstract class ChangeSourceSpecTransformer<E extends ChangeSource, S extends ChangeSourceSpec> {
  public abstract E getEntity(ServiceEnvironmentParams environmentParams, ChangeSourceDTO changeSourceDTO);

  public final ChangeSourceDTO getDTO(E changeSource) {
    return ChangeSourceDTO.builder()
        .spec(getSpec(changeSource))
        .enabled(changeSource.isEnabled())
        .name(changeSource.getName())
        .identifier(changeSource.getIdentifier())
        .type(changeSource.getType())
        .build();
  }

  protected abstract S getSpec(E changeSource);
}
