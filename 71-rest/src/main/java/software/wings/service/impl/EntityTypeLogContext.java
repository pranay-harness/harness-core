package software.wings.service.impl;

import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import software.wings.beans.EntityType;

public class EntityTypeLogContext extends AutoLogContext {
  public static final String ENTITY_TYPE = "entityType";
  public static final String ENTITY_ID = "entityId";

  public EntityTypeLogContext(EntityType entityType, String entityId, String accountId, OverrideBehavior behavior) {
    super(NullSafeImmutableMap.<String, String>builder()
              .put(ENTITY_TYPE, entityType.name())
              .putIfNotNull(ENTITY_ID, entityId)
              .putIfNotNull(AccountLogContext.ID, accountId)
              .build(),
        behavior);
  }
}
