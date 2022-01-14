package software.wings.api;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.context.ContextElementType.CLUSTER;

import io.harness.annotations.dev.TargetModule;
import io.harness.context.ContextElementType;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(_957_CG_BEANS)
public class ClusterElement implements ContextElement {
  private String uuid;
  private String name;
  private DeploymentType deploymentType;
  private String infraMappingId;

  @Override
  public ContextElementType getElementType() {
    return CLUSTER;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }
}
