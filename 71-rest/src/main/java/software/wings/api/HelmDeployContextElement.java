package software.wings.api;

import io.harness.context.ContextElementType;
import lombok.Builder;
import lombok.Data;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;

/**
 * Created by anubhaw on 4/3/18.
 */
@Data
@Builder
public class HelmDeployContextElement implements ContextElement {
  private String releaseName;
  private Integer previousReleaseRevision;
  private Integer newReleaseRevision;
  private String commandFlags;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.HELM_DEPLOY;
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
