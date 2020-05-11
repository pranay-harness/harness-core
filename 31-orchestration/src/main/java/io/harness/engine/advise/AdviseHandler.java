package io.harness.engine.advise;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.Advise;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface AdviseHandler<T extends Advise> {
  void handleAdvise(Ambiance ambiance, T advise);
}
