package io.harness.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HttpRequestInfo {
  String requestMethod;
}
