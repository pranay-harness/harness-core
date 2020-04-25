package io.harness.utils.levels;

import io.harness.state.io.ambiance.Level;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PhaseTestLevel implements Level {
  String name = "PHASE";
  int order;
}
