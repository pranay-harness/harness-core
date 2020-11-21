package software.wings.api.terraform;

import io.harness.context.ContextElementType;

import software.wings.beans.NameValuePair;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class TerraformProvisionInheritPlanElement implements ContextElement {
  private String entityId;
  private String provisionerId;
  private List<String> targets;
  private List<String> tfVarFiles;
  private String sourceRepoSettingId;
  private String sourceRepoReference;
  private List<NameValuePair> variables;
  private List<NameValuePair> backendConfigs;
  private List<NameValuePair> environmentVariables;
  private String workspace;
  private String delegateTag;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.TERRAFORM_INHERIT_PLAN;
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
