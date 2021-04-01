package io.harness.gitsync.functor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.NGDTO;
import io.harness.gitsync.beans.NGPersistentEntity;

@FunctionalInterface
@OwnedBy(DX)
public interface NgDtoToEntityFunctor<Y extends NGDTO, E extends NGPersistentEntity> {
  E apply(Y yaml);
}