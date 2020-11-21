package io.harness.context;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MdcGlobalContextData implements GlobalContextData {
  public static final String MDC_ID = "MDC";

  private Map<String, String> map;

  @Override
  public String getKey() {
    return MDC_ID;
  }
}
