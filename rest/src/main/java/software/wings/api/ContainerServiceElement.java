package software.wings.api;

import static software.wings.sm.ContextElementType.CONTAINER_SERVICE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.ResizeStrategy;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.Map;

/**
 * Created by rishi on 4/11/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class ContainerServiceElement implements ContextElement {
  private String uuid;
  private String name;
  private int maxInstances;
  private int serviceSteadyStateTimeout;
  private ResizeStrategy resizeStrategy;
  private String clusterName;
  private String namespace;
  private DeploymentType deploymentType;
  private String infraMappingId;

  @Override
  public ContextElementType getElementType() {
    return CONTAINER_SERVICE;
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
