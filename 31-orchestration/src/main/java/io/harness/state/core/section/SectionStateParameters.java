package io.harness.state.core.section;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StateParameters;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class SectionStateParameters implements StateParameters {
  String childNodeId;
}
