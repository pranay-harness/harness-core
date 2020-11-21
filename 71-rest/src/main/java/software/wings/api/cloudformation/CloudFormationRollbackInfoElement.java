package software.wings.api.cloudformation;

import io.harness.context.ContextElementType;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CloudFormationRollbackInfoElement implements CloudFormationElement {
  private String awsConfigId;
  private String region;
  private boolean stackExisted;
  private String oldStackBody;
  private String stackNameSuffix;
  private String customStackName;
  private String provisionerId;
  private Map<String, String> oldStackParameters;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.CLOUD_FORMATION_ROLLBACK;
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
