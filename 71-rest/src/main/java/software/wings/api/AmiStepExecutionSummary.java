package software.wings.api;

import software.wings.beans.InstanceUnitType;
import software.wings.sm.StepExecutionSummary;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by anubhaw on 12/22/17.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class AmiStepExecutionSummary extends StepExecutionSummary {
  private int instanceCount;
  private InstanceUnitType instanceUnitType;
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;

  public AmiServiceDeployElement getRollbackAmiServiceElement() {
    return AmiServiceDeployElement.builder()
        .instanceCount(instanceCount)
        .instanceUnitType(instanceUnitType)
        .newInstanceData(newInstanceData)
        .oldInstanceData(oldInstanceData)
        .build();
  }
}
