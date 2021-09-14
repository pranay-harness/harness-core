/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.iterator;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

@OwnedBy(PL)
public interface PersistentIterable extends PersistentEntity, UuidAccess {
  Long obtainNextIteration(String fieldName);
}
