package io.harness.refrences;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RefType {
  public static final String SWEEPING_OUTPUT = "SWEEPING_OUTPUT";

  String type;
}
