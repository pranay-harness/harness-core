package io.harness.utils.levels;

import io.harness.state.io.ambiance.Level;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SectionTestLevel implements Level {
  String name = "SECTION";
  int order = 1;
}
