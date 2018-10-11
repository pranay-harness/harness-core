package software.wings.api;

import static software.wings.sm.ContextElementType.CONTAINER_SERVICE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.container.Label;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.List;
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
  private String image;
  private boolean useFixedInstances;
  private int fixedInstances;
  private int maxInstances;
  private int serviceSteadyStateTimeout;
  private ResizeStrategy resizeStrategy;
  private String clusterName;
  private String namespace;
  private DeploymentType deploymentType;
  private String infraMappingId;
  private boolean useAutoscaler;
  private String autoscalerYaml;
  private int minAutoscaleInstances;
  private int maxAutoscaleInstances;
  private int targetCpuUtilizationPercentage;
  private String customMetricYamlConfig;
  private boolean useIstioRouteRule;
  private List<String[]> activeServiceCounts;
  private List<String[]> trafficWeights;
  private String controllerNamePrefix;
  private List<Label> lookupLabels;

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
