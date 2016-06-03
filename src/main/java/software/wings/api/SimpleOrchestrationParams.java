/**
 *
 */
package software.wings.api;

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
  public static final String SIMPLE_ORCHESTRATION_PARAMS = "SIMPLE_ORCHESTRATION_PARAMS";

  private String serviceId;
  private List<String> instanceIds;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARAM;
  }

  @Override
  public String getName() {
    return SIMPLE_ORCHESTRATION_PARAMS;
  }

  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(SIMPLE_ORCHESTRATION_PARAMS + ".serviceId", serviceId);
    map.put(SIMPLE_ORCHESTRATION_PARAMS + ".instanceIds", instanceIds);
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
