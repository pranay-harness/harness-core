package io.harness.context;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

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
