package io.harness.remote.client;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceHttpClientConfig {
  String baseUrl;
  @Builder.Default long connectTimeOutSeconds = 15;
  @Builder.Default long readTimeOutSeconds = 15;
}
