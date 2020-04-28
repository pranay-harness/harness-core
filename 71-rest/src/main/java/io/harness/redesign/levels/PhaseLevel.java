package io.harness.redesign.levels;

import io.harness.ambiance.Level;
import io.harness.annotations.Produces;
import lombok.Value;

@Value
@Produces(Level.class)
public class PhaseLevel implements Level {
  public static final String LEVEL_NAME = "PHASE";

  String name = LEVEL_NAME;
  int order = 1;
}
