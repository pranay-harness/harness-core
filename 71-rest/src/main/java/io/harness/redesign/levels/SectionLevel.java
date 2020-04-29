package io.harness.redesign.levels;

import io.harness.ambiance.Level;
import io.harness.ambiance.LevelType;
import io.harness.annotations.Produces;
import lombok.Value;

@Value
@Produces(Level.class)
public class SectionLevel implements Level {
  public static final LevelType LEVEL_TYPE = LevelType.builder().type("SECTION").build();

  LevelType type = LEVEL_TYPE;
  int order = 2;
}
