package io.harness.ambiance.dev;

import io.harness.ambiance.Level;
import io.harness.ambiance.LevelType;

public class DefaultLevel implements Level {
  @Override
  public LevelType getType() {
    return LevelType.builder().type("DEFAULT").build();
  }

  @Override
  public int getOrder() {
    return 0;
  }
}
