package software.wings.api;

import lombok.Builder;
import lombok.Data;
import software.wings.common.Constants;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.Map;

@Data
@Builder
public class RouteUpdateRollbackElement implements ContextElement {
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
    return Constants.ROUTE_UPDATE_ROLLBACK_REQUEST_PARAM;
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
