package io.harness.redesign.levels;

import io.harness.annotations.ProducesLevel;
import io.harness.state.io.ambiance.Level;
import lombok.Value;

@Value
@ProducesLevel
public class PhaseLevel implements Level {
  public static final String LEVEL_NAME = "PHASE";

  String name = LEVEL_NAME;
  int order = 1;
}
