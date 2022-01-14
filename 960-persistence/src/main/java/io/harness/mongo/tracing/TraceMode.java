package io.harness.mongo.tracing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public enum TraceMode {
  ENABLED,
  DISABLED;
}
