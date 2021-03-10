package io.harness.request;

import io.harness.context.GlobalContextData;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RequestContextData implements GlobalContextData {
  public static final String REQUEST_CONTEXT = "REQUEST_CONTEXT";

  RequestContext requestContext;

  @Override
  public String getKey() {
    return REQUEST_CONTEXT;
  }
}
