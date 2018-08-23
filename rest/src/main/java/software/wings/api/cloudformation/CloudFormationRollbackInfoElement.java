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
public class CloudFormationRollbackInfoElement extends CloudFormationElement {
  private String awsConfigId;
  private String region;
  private boolean stackExisted;
  private String oldStackBody;
  private String stackNameSuffix;
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