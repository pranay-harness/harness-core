package io.harness.context;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Value
@Builder
@TypeAlias("MdcGlobalContextData")
public class MdcGlobalContextData implements GlobalContextData {
  public static final String MDC_ID = "MDC";

  private Map<String, String> map;

  @Override
  public String getKey() {
    return MDC_ID;
  }
}
