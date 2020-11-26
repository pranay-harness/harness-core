package io.harness.iterator;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public interface PersistentRegularIterable extends PersistentIterable {
  void updateNextIteration(String fieldName, long nextIteration);
}
