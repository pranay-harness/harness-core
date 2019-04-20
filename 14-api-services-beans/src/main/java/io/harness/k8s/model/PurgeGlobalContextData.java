package io.harness.k8s.model;

import io.harness.context.GlobalContextData;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PurgeGlobalContextData implements GlobalContextData {
  public static final String PURGE_OP = "PURGE_OP";

  @Override
  public String getKey() {
    return PURGE_OP;
  }
}
