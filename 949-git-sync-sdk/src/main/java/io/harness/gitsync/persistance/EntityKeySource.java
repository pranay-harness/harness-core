package io.harness.gitsync.persistance;

import io.harness.eventsframework.schemas.entity.EntityScopeInfo;

public interface EntityKeySource {
  boolean fetchKey(EntityScopeInfo baseNGAccess);

  void updateKey(EntityScopeInfo entityReference);
}
