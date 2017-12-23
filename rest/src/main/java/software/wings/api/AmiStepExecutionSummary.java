package software.wings.api;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.ResizeStrategy;
import software.wings.sm.StepExecutionSummary;

/**
 * Created by anubhaw on 12/22/17.
 */
@Data
@Builder
public class AmiStepExecutionSummary extends StepExecutionSummary {
  private String activityId;
  private String commandName;
  private int instanceCount;
  private InstanceUnitType instanceUnitType;
  private String newAutoScalingGroupName;
  private String oldAutoScalingGroupName;
  private Integer newVersion;
  private Integer autoScalingSteadyStateTimeout;
  private Integer maxInstances;
  private ResizeStrategy resizeStrategy;

  public AmiServiceSetupElement getRollbackAmiServiceElement() {
    return AmiServiceSetupElement.builder()
        .commandName(commandName)
        .instanceCount(instanceCount)
        .instanceUnitType(instanceUnitType)
        .newAutoScalingGroupName(oldAutoScalingGroupName)
        .oldAutoScalingGroupName(newAutoScalingGroupName)
        .autoScalingSteadyStateTimeout(autoScalingSteadyStateTimeout)
        .maxInstances(maxInstances)
        .resizeStrategy(resizeStrategy)
        .build();
  }
}
