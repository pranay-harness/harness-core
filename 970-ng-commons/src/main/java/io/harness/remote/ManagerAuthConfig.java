package io.harness.remote;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManagerAuthConfig {
  String jwtAuthSecret;
}
