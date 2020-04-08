package io.harness.adviser;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class AdviserObtainment {
  AdviserType type;
  AdviserParameters parameters;
}
