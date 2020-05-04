package io.harness.data;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class OutcomeType {
  @NonNull String type;
}
