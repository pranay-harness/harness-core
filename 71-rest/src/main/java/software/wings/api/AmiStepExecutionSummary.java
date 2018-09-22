package software.wings.api;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.InstanceUnitType;
import software.wings.sm.StepExecutionSummary;

import java.util.List;

/**
 * Created by anubhaw on 12/22/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
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
