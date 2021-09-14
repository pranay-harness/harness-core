/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo;

import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EntityLogContext extends AutoLogContext {
  public static final String ENTITY_CLASS = "entityClass";

  public EntityLogContext(PersistentEntity entity, OverrideBehavior behavior) {
    super(NullSafeImmutableMap.<String, String>builder()
              .put(ENTITY_CLASS, entity.getClass().getName())
              .putIfNotNull(entity instanceof UuidAccess ? ((UuidAccess) entity).logKeyForId() : "",
                  entity instanceof UuidAccess ? ((UuidAccess) entity).getUuid() : null)
              .putIfNotNull(AccountLogContext.ID,
                  entity instanceof AccountAccess ? ((AccountAccess) entity).getAccountId() : null)
              .build(),
        behavior);
  }
}
