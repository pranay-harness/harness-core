package io.harness.utils.levels;

import io.harness.ambiance.Level;
import io.harness.ambiance.LevelType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StepTestLevel implements Level {
  public static final LevelType LEVEL_TYPE = LevelType.builder().type("STEP_TEST").build();
  LevelType type = LEVEL_TYPE;
  int order = 3;
}
