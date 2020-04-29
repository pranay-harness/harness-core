package io.harness.utils.levels;

import io.harness.ambiance.Level;
import io.harness.ambiance.LevelType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PhaseTestLevel implements Level {
  public static final LevelType LEVEL_TYPE = LevelType.builder().type("PHASE_TEST").build();
  LevelType type = LEVEL_TYPE;
  int order = 1;
}
