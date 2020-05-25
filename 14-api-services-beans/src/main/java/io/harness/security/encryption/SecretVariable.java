package io.harness.security.encryption;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode
public class SecretVariable {
  @NonNull private String name;
  @NonNull private String value;
}
