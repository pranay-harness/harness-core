package software.wings.api;

import io.harness.context.ContextElementType;
import lombok.Builder;
import lombok.Value;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * The type Service instance ids param.
 */
@Value
@Builder
public class InstanceElementListParam implements ContextElement {
  public static final String INSTANCE_LIST_PARAMS = "INSTANCE_LIST_PARAMS";

  private List<InstanceElement> instanceElements;
  private List<PcfInstanceElement> pcfInstanceElements;
  private List<PcfInstanceElement> pcfOldInstanceElements;

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
    return INSTANCE_LIST_PARAMS;
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
