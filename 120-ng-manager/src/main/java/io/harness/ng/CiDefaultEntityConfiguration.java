package io.harness.ng;

import io.harness.secret.ConfigSecret;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CiDefaultEntityConfiguration {
  @ConfigSecret String harnessImageUseName;
  @ConfigSecret String harnessImagePassword;
}
