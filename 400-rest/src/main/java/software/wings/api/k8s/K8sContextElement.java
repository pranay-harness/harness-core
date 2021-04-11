package software.wings.api.k8s;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.context.ContextElementType;

import software.wings.beans.TaskType;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(_870_CG_ORCHESTRATION)
@OwnedBy(CDP)
public class K8sContextElement implements ContextElement {
  String releaseName;
  Integer releaseNumber;
  Integer targetInstances;
  TaskType currentTaskType;
  List<String> delegateSelectors;

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
