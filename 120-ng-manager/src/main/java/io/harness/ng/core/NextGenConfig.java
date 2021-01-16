package io.harness.ng.core;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NextGenConfig {
  String managerServiceSecret;
  String cvNgServiceSecret;
  String userVerificationSecret;
  String ngManagerServiceSecret;
  String jwtAuthSecret;
}
