package io.harness.remote;

import io.harness.secret.ConfigSecret;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NextGenConfig {
  @ConfigSecret String managerServiceSecret;
  @ConfigSecret String userVerificationSecret;
  @ConfigSecret String ngManagerServiceSecret;
  @ConfigSecret String pipelineServiceSecret;
  @ConfigSecret String jwtAuthSecret;
  @ConfigSecret String jwtIdentityServiceSecret;
  @ConfigSecret String ciManagerSecret;
  @ConfigSecret String ceNextGenServiceSecret;
}