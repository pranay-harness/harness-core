package software.wings.api.artifact;

import io.harness.context.ContextElementType;
import lombok.Builder;
import lombok.Value;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class ServiceArtifactElement implements ContextElement {
  private String uuid;
  private String name;
  private List<String> serviceIds;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.ARTIFACT;
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
