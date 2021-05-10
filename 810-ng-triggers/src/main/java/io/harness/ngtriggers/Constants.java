package io.harness.ngtriggers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface Constants {
  String PR = "PR";
  String PUSH = "PUSH";
  String TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER = ":";
}
