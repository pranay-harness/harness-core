package io.harness.mongo;

import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.AccountLogContext;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

public class EntityLogContext extends AutoLogContext {
  public EntityLogContext(PersistentEntity entity, OverrideBehavior behavior) {
    super(NullSafeImmutableMap.<String, String>builder()
              .put("class", entity.getClass().getName())
              .putIfNotNull(entity.getClass().getSimpleName() + "Id",
                  entity instanceof UuidAccess ? ((UuidAccess) entity).getUuid() : null)
              .putIfNotNull(AccountLogContext.ID,
                  entity instanceof AccountAccess ? ((AccountAccess) entity).getAccountId() : null)
              .build(),
        behavior);
  }
}
