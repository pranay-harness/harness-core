package io.harness.state.core.dummy;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class DummySectionStepParameters implements StepParameters {
  String childNodeId;
  DummySectionStepTransput data;
}
