package io.harness.state.core.section;

import io.harness.state.io.StateParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SectionStateParameters implements StateParameters {
  String childNodeId;
}
