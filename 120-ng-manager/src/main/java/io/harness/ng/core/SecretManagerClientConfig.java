package io.harness.ng.core;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SecretManagerClientConfig {
  String baseUrl;
  @Builder.Default long connectTimeOutSeconds = 15;
  @Builder.Default long readTimeOutSeconds = 15;
}
