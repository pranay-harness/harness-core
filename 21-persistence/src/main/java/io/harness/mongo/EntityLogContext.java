package io.harness.mongo;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AutoLogContext;
import io.harness.persistence.UuidAccess;

public class EntityLogContext extends AutoLogContext {
  public EntityLogContext(UuidAccess entity, OverrideBehavior behavior) {
    super(ImmutableMap.<String, String>builder()
              .put("class", entity.getClass().getName())
              .put("uuid", entity.getUuid())
              .build(),
        behavior);
  }
}
