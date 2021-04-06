package io.harness.request;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Value
@Builder
@TypeAlias("RequestContextData")
public class RequestContextData implements GlobalContextData {
  public static final String REQUEST_CONTEXT = "REQUEST_CONTEXT";

  RequestContext requestContext;

  @Override
  public String getKey() {
    return REQUEST_CONTEXT;
  }
}
