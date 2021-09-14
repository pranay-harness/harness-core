/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.api;

import org.mongodb.morphia.query.UpdateOperations;

public interface UpdatableEntity<T, D> {
  void setUpdateOperations(UpdateOperations<T> updateOperations, D dto);
}
