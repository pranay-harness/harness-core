package software.wings.api;

import software.wings.common.Constants;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rishi on 4/5/17.
 */
public class ServiceInstanceArtifactParam implements ContextElement {
  private Map<String, String> instanceArtifactMap = new HashMap<>();

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARAM;
  }

  @Override
  public String getUuid() {
    return null;
  }

  @Override
  public String getName() {
    return Constants.SERVICE_INSTANCE_ARTIFACT_PARAMS;
  }

  public Map<String, String> getInstanceArtifactMap() {
    return instanceArtifactMap;
  }

  public void setInstanceArtifactMap(Map<String, String> instanceArtifactMap) {
    this.instanceArtifactMap = instanceArtifactMap;
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
