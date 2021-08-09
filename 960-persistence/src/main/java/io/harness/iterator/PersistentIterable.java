package io.harness.iterator;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

@OwnedBy(PL)
public interface PersistentIterable extends PersistentEntity, UuidAccess {
  Long obtainNextIteration(String fieldName);
}
