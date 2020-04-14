package io.harness.refrences;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class RefType {
  public static final String SWEEPING_OUTPUT = "SWEEPING_OUTPUT";

  String type;
}
