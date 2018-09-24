package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.ResizeStrategy;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.Map;

/**
 * Created by anubhaw on 12/20/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmiServiceSetupElement implements ContextElement {
  private String uuid;
  private String name;
  private String commandName;
  private int instanceCount;
  private String newAutoScalingGroupName;
  private String oldAutoScalingGroupName;
  private Integer autoScalingSteadyStateTimeout;
  private Integer maxInstances;
  private int minInstances;
  private int oldAsgMinSize;
  private int oldAsgDesiredCapacity;
  private ResizeStrategy resizeStrategy;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.AMI_SERVICE_SETUP;
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
