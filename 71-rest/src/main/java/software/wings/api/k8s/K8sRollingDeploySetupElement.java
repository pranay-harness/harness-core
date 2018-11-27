package software.wings.api.k8s;

import lombok.Builder;
import lombok.Data;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.Map;

@Data
@Builder
public class K8sRollingDeploySetupElement implements ContextElement {
  String releaseName;
  int releaseNumber;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.K8S_ROLLING_DEPLOY_SETUP;
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
