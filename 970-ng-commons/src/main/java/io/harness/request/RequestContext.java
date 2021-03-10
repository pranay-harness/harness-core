package io.harness.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RequestContext {
  String requestMethod;
  String clientIP;
}
