package io.harness.state.inspection;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DummiesStateInspectionData implements StateInspectionData {
  private String value;

  @Override
  public String key() {
    return "dummies";
  }
}
