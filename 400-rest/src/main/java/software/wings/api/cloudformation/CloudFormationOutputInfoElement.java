package software.wings.api.cloudformation;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.context.ContextElementType;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
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
