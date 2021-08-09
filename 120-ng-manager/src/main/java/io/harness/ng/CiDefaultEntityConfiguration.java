package io.harness.ng;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CiDefaultEntityConfiguration {
  String harnessImageUseName;
  String harnessImagePassword;
}
