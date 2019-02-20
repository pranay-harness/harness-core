package software.wings.api.k8s;

import io.harness.context.ContextElementType;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.TaskType;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;

@Data
@Builder
public class K8sContextElement implements ContextElement {
  String releaseName;
  Integer releaseNumber;
  Integer targetInstances;
  TaskType currentTaskType;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.K8S;
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
