package io.harness.gitsync.functor;

import io.harness.gitsync.beans.NGDTO;
import io.harness.gitsync.beans.NGPersistentEntity;

@FunctionalInterface
public interface NgEntityToDtoFunctor<E extends NGPersistentEntity, Y extends NGDTO> {
  Y apply(E e);
}
