package software.wings.api;

import lombok.Builder;
import lombok.Data;
import software.wings.common.Constants;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 4/11/17.
 */
@Data
@Builder
public class ContainerUpgradeRequestElement implements ContextElement {
  private ContainerServiceElement containerServiceElement;
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;

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
    return Constants.CONTAINER_UPGRADE_REQUEST_PARAM;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }
}
