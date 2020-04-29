package io.harness.redesign.levels;

import io.harness.ambiance.Level;
import io.harness.ambiance.LevelType;
import io.harness.annotations.Produces;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Produces(Level.class)
public class StepLevel implements Level {
  public static final LevelType LEVEL_TYPE = LevelType.builder().type("STEP").build();

  LevelType type = LEVEL_TYPE;
  int order = 3;
}
