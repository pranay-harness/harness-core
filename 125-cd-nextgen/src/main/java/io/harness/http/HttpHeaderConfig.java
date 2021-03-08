package io.harness.http;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpHeaderConfig {
  String key;
  String value;
}
