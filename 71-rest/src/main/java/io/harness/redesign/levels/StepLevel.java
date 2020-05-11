package io.harness.redesign.levels;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Level;
import io.harness.ambiance.LevelType;
import io.harness.annotations.Produces;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Produces(Level.class)
public class StepLevel implements Level {
  public static final LevelType LEVEL_TYPE = LevelType.builder().type("STEP").build();

  LevelType type = LEVEL_TYPE;
  int order = 3;
}
