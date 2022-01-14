package io.harness.cvng.core;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NGManagerServiceConfig {
  String managerServiceSecret;
  String ngManagerUrl;
}
