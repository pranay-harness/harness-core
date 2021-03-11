package io.harness.gitsync.functor;

import io.harness.gitsync.beans.NGDTO;
import io.harness.gitsync.beans.NGPersistentEntity;

@FunctionalInterface
public interface NgDtoToEntityFunctor<Y extends NGDTO, E extends NGPersistentEntity> {
  E apply(Y yaml);
}