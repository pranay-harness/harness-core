package io.harness.ambiance.dev;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Level;
import io.harness.ambiance.LevelType;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
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
