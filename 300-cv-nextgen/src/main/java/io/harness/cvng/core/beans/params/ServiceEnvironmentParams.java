package io.harness.cvng.core.beans.params;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Value
@SuperBuilder
public class ServiceEnvironmentParams extends ProjectParams {
  @NonNull String serviceIdentifier;
  @NonNull String environmentIdentifier;
}
