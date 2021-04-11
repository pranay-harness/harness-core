package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;

@OwnedBy(DX)
public interface EntityKeySource {
  boolean fetchKey(EntityScopeInfo baseNGAccess);

  void updateKey(EntityScopeInfo entityReference);
}
