/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.transformer.changeEvent;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.beans.change.ChangeEventDTO;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

public class ChangeEventEntityAndDTOTransformer {
  @Inject Injector injector;

  public Activity getEntity(ChangeEventDTO changeEventDTO) {
    ChangeEventMetaDataTransformer changeEventMetaDataTransformer = injector.getInstance(Key.get(
        ChangeEventMetaDataTransformer.class, Names.named(changeEventDTO.getChangeEventMetaData().getType().name())));
    return changeEventMetaDataTransformer.getEntity(changeEventDTO);
  }

  public ChangeEventDTO getDto(Activity changeEvent) {
    ChangeEventMetaDataTransformer changeEventMetaDataTransformer =
        injector.getInstance(Key.get(ChangeEventMetaDataTransformer.class, Names.named(changeEvent.getType().name())));
    return changeEventMetaDataTransformer.getDTO(changeEvent);
  }
}
