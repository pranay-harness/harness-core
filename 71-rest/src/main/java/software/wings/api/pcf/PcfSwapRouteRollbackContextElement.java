package software.wings.api.pcf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.context.ContextElementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PcfSwapRouteRollbackContextElement implements ContextElement {
  private String uuid;
  private String name;
  private PcfRouteUpdateRequestConfigData pcfRouteUpdateRequestConfigData;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PCF_ROUTE_SWAP_ROLLBACK;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }
}
