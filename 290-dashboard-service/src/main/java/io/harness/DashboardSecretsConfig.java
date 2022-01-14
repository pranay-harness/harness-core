package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.PL)
public class DashboardSecretsConfig {
  String ngManagerServiceSecret;
  String pipelineServiceSecret;
  String jwtAuthSecret;
  String jwtIdentityServiceSecret;
}
