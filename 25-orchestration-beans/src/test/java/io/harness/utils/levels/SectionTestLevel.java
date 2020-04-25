package io.harness.utils.levels;

import io.harness.state.io.ambiance.Level;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SectionTestLevel implements Level {
  public static final String LEVEL_NAME = "SECTION_TEST";
  String name = LEVEL_NAME;
  int order = 2;
}
