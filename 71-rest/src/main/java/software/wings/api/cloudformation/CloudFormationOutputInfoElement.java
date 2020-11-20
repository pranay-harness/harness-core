package software.wings.api.cloudformation;

import io.harness.context.ContextElementType;
import lombok.Builder;
import lombok.Data;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class CloudFormationOutputInfoElement implements CloudFormationElement {
  private Map<String, Object> newStackOutputs;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.CLOUD_FORMATION_PROVISION;
  }

  @Override
  public String getUuid() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put("cloudformation", newStackOutputs);
    return map;
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }

  public void mergeOutputs(Map<String, Object> newMap) {
    if (newStackOutputs == null) {
      newStackOutputs = new HashMap<>();
    }
    newStackOutputs.putAll(newMap);
  }
}
