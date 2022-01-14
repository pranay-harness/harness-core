package io.harness.grpc.auth;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceInfo {
  String id;
  String secret;
}
