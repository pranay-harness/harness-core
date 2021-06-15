package io.harness.engine.observers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.observers.beans.OrchestrationStartInfo;

@OwnedBy(HarnessTeam.PIPELINE)
public interface OrchestrationStartObserver {
  void onStart(OrchestrationStartInfo orchestrationStartInfo);
}
