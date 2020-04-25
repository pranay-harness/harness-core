package io.harness.utils.levels;

import io.harness.state.io.ambiance.Level;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PhaseTestLevel implements Level {
  public static final String LEVEL_NAME = "PHASE_TEST";
  String name = LEVEL_NAME;
  int order = 1;
}
