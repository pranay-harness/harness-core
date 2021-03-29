package io.harness.gitsync.persistance;

import io.harness.common.EntityReference;

public interface EntityKeySource {
  boolean fetchKey(EntityReference baseNGAccess);

  void updateKey(EntityReference entityReference);
}
