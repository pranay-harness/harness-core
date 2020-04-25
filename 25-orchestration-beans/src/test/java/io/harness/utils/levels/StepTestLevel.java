package io.harness.utils.levels;

import io.harness.state.io.ambiance.Level;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StepTestLevel implements Level {
  String name = "STEP";
  int order = 2;
}
