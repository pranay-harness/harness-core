package io.harness.cvng.core.transformer.changeSource;

import io.harness.cvng.core.beans.EnvironmentParams;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.entities.changeSource.ChangeSource;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

public class ChangeSourceEntityAndDTOTransformer {
  @Inject Injector injector;

  public ChangeSource getEntity(EnvironmentParams environmentParams, ChangeSourceDTO changeSourceDTO) {
    ChangeSourceSpecTransformer changeSourceSpecTransformer =
        injector.getInstance(Key.get(ChangeSourceSpecTransformer.class, Names.named(changeSourceDTO.getType().name())));
    return changeSourceSpecTransformer.getEntity(environmentParams, changeSourceDTO);
  }

  public ChangeSourceDTO getDto(ChangeSource changeSource) {
    ChangeSourceSpecTransformer changeSourceSpecTransformer =
        injector.getInstance(Key.get(ChangeSourceSpecTransformer.class, Names.named(changeSource.getType().name())));
    return changeSourceSpecTransformer.getDTO(changeSource);
  }
}
