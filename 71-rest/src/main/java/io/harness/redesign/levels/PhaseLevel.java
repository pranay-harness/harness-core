package io.harness.redesign.levels;

import io.harness.ambiance.Level;
import io.harness.annotations.ProducesLevel;
import lombok.Value;

@Value
@ProducesLevel
public class PhaseLevel implements Level {
  public static final String LEVEL_NAME = "PHASE";

  String name = LEVEL_NAME;
  int order = 1;
}
