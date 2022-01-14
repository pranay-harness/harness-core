package io.harness.platform;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlatformSecrets {
  String ngManagerServiceSecret;
  String jwtAuthSecret;
  String jwtIdentityServiceSecret;
}
