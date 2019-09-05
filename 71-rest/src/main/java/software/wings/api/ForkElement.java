package software.wings.api;

import io.harness.context.ContextElementType;
import lombok.Builder;
import lombok.Value;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;

/**
 * The Class ForkElement.
 */
@Value
@Builder
public class ForkElement implements ContextElement {
  private String parentId;
  private String stateName;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.FORK;
  }
  @Override
  public String getUuid() {
    return parentId + "-fork-" + stateName;
  }

  @Override
  public String getName() {
    return "Fork-" + stateName;
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
