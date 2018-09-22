package software.wings.api.cloudformation;

import com.google.common.collect.Maps;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class CloudFormationOutputInfoElement extends CloudFormationElement {
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
    Map<String, Object> map = Maps.newHashMap();
    map.put("cloudformation", newStackOutputs);
    return map;
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }

  public void mergeOutputs(Map<String, Object> newMap) {
    newStackOutputs.putAll(newMap);
  }
}