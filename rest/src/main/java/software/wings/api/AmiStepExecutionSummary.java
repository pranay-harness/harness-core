package software.wings.api;

import lombok.Builder;
import lombok.Data;
import software.wings.sm.StepExecutionSummary;

import java.util.List;

/**
 * Created by anubhaw on 12/22/17.
 */
@Data
@Builder
public class AmiStepExecutionSummary extends StepExecutionSummary {
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;

  public AmiServiceDeployElement getRollbackAmiServiceElement() {
    return AmiServiceDeployElement.builder().newInstanceData(newInstanceData).oldInstanceData(oldInstanceData).build();
  }
}
