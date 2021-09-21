package io.harness;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardSecretsConfig {
    String ngManagerServiceSecret;
    String pipelineServiceSecret;
    String jwtAuthSecret;
    String jwtIdentityServiceSecret;
}
