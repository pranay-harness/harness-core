package io.harness.platform;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlatformSecrets {
  String ngManagerServiceSecret;
  String jwtAuthSecret;
  String jwtIdentityServiceSecret;
}
