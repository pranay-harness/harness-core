/**
 *
 */
package software.wings.api;

import software.wings.common.Constants;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Rishi
 *
 */
public class SimpleOrchestrationParams implements ContextElement {
  private String serviceId;
  private List<String> instanceIds;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARAM;
  }

  @Override
  public String getName() {
    return Constants.SIMPLE_ORCHESTRATION_PARAMS;
  }

  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(Constants.SIMPLE_ORCHESTRATION_PARAMS + ".serviceId", serviceId);
    map.put(Constants.SIMPLE_ORCHESTRATION_PARAMS + ".instanceIds", instanceIds);
    return map;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public List<String> getInstanceIds() {
    return instanceIds;
  }

  public void setInstanceIds(List<String> instanceIds) {
    this.instanceIds = instanceIds;
  }
}
