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
  String userVerificationSecret;
  String ngManagerServiceSecret;
  String pipelineServiceSecret;
  String jwtAuthSecret;
  String jwtIdentityServiceSecret;
  String ciManagerSecret;
}
