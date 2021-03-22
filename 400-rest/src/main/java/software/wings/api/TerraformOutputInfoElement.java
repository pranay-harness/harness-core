package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.ContextElementType;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@OwnedBy(CDP)
public class TerraformOutputInfoElement implements ContextElement {
  private Map<String, Object> outputVariables;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.TERRAFORM_PROVISION;
  }

  @Override
  public String getUuid() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  /**
   * @deprecated use {@link software.wings.api.terraform.TerraformOutputVariables} for storing outputs
   */
  @Override
  @Deprecated
  public Map<String, Object> paramMap(ExecutionContext context) {
    HashMap<String, Object> paramMap = new HashMap<>();
    paramMap.put("terraform", outputVariables);
    return paramMap;
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }

  public void addOutPuts(Map<String, Object> newMap) {
    if (outputVariables == null) {
      outputVariables = new HashMap<>();
    }
    outputVariables.putAll(newMap);
  }
}
