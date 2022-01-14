package io.harness.engine.observers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public interface NodeExecutionStartObserver {
  void onNodeStart(NodeStartInfo nodeStartInfo);
}
