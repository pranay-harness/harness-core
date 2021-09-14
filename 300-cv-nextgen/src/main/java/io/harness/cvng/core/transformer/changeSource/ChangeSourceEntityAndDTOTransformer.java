/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.transformer.changeSource;

import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.ChangeSource;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

public class ChangeSourceEntityAndDTOTransformer {
  @Inject Injector injector;

  public ChangeSource getEntity(ServiceEnvironmentParams environmentParams, ChangeSourceDTO changeSourceDTO) {
    ChangeSourceSpecTransformer changeSourceSpecTransformer = injector.getInstance(
        Key.get(ChangeSourceSpecTransformer.class, Names.named(changeSourceDTO.getSpec().getType().name())));

    return changeSourceSpecTransformer.getEntity(environmentParams, changeSourceDTO);
  }

  public ChangeSourceDTO getDto(ChangeSource changeSource) {
    ChangeSourceSpecTransformer changeSourceSpecTransformer =
        injector.getInstance(Key.get(ChangeSourceSpecTransformer.class, Names.named(changeSource.getType().name())));
    return changeSourceSpecTransformer.getDTO(changeSource);
  }
}
