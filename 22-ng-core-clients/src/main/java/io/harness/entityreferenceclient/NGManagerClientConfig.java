package io.harness.entityreferenceclient;

import com.google.inject.Singleton;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class NGManagerClientConfig {
  String baseUrl;
  @Builder.Default long connectTimeOutSeconds = 15;
  @Builder.Default long readTimeOutSeconds = 15;
}
