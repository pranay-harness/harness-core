package software.wings.api.cloudformation;

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
public class CloudFormationDeleteStackElement extends CloudFormationElement {
  @Override
  public ContextElementType getElementType() {
    return ContextElementType.CLOUD_FORMATION_DEPROVISION;
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
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }
}