package io.harness.engine.advise;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.advisers.AdviserResponse;
import io.harness.pms.ambiance.Ambiance;

@OwnedBy(CDC)
public interface AdviserResponseHandler {
  void handleAdvise(Ambiance ambiance, AdviserResponse advise);
}
