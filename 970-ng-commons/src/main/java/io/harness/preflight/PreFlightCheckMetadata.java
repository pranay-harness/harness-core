package io.harness.preflight;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PreFlightCheckMetadata {
  String FQN = "fqn";
  String EXPRESSION = "expression";
}
