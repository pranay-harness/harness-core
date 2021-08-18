package io.harness.cvng.core.beans;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Builder
@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnvironmentParams {
  @NonNull ProjectParams projectParams;
  @NonNull String serviceIdentifier;
  @NonNull String envIdentifier;
}
