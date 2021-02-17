package io.harness.cvng.core.services.api;

import org.mongodb.morphia.query.UpdateOperations;

public interface UpdatableEntity<T, D> {
  void setUpdateOperations(UpdateOperations<T> updateOperations, D dto);
}
