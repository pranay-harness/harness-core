package io.harness.gitsync.functor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.NGPersistentEntity;
import io.harness.gitsync.beans.YamlDTO;

@FunctionalInterface
@OwnedBy(DX)
public interface NgEntityToDtoFunctor<E extends NGPersistentEntity, Y extends YamlDTO> {
  Y apply(E e);
}
