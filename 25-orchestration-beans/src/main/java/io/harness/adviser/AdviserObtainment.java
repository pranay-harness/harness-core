package io.harness.adviser;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@Redesign
public class AdviserObtainment {
  @NonNull AdviserType type;
  AdviserParameters parameters;
}
