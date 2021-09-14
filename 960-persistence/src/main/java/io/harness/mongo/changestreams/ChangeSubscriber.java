/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo.changestreams;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

@OwnedBy(PL)
@FunctionalInterface
public interface ChangeSubscriber<T extends PersistentEntity> {
  void onChange(ChangeEvent<T> changeEvent);
}
