package software.wings.api;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;

import io.harness.annotations.dev.TargetModule;
import io.harness.context.ContextElementType;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(_957_CG_BEANS)
public class RouteUpdateRollbackElement implements ContextElement {
  private static final String ROUTE_UPDATE_ROLLBACK_REQUEST_PARAM = "ROUTE_UPDATE_ROLLBACK_REQUEST_PARAM";

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARAM;
  }

  @Override
  public String getUuid() {
    return null;
  }

  @Override
  public String getName() {
    return ROUTE_UPDATE_ROLLBACK_REQUEST_PARAM;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }
}
