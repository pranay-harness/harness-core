package software.wings.api;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;

import io.harness.annotations.dev.TargetModule;
import io.harness.context.ContextElementType;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(_957_CG_BEANS)
public class EcsSetupElement implements ContextElement {
  Integer serviceSteadyStateTimeout;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.ECS_SERVICE_SETUP;
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
    return this;
  }
}
