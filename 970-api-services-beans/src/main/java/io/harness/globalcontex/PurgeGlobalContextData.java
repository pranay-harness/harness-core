package io.harness.globalcontex;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Value
@Builder
@TypeAlias("PurgeGlobalContextData")
public class PurgeGlobalContextData implements GlobalContextData {
  public static final String PURGE_OP = "PURGE_OP";

  @Override
  public String getKey() {
    return PURGE_OP;
  }
}
