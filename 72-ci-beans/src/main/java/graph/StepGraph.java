package graph;

import io.harness.beans.steps.CIStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepGraph implements Graph<CIStep> {
  // TODO Convert step list to proper graph
  private List<CIStep> ciSteps;

  public static final String NIL_NODE = "00000000-0000-0000-0000-000000000000";

  public String getStartNodeUuid() {
    if (ciSteps.isEmpty()) {
      throw new IllegalStateException("Steps list can not be empty");
    }
    return ciSteps.get(0).getStepMetadata().getUuid();
  }

  public static boolean isNILStepUuId(String uuId) {
    return uuId.equals(NIL_NODE);
  }

  public String getNextNodeUuid(CIStep currentStep) {
    int index = ciSteps.indexOf(currentStep);

    if (index >= 0 && index < ciSteps.size() - 1) {
      return ciSteps.get(index + 1).getStepMetadata().getUuid();
    } else {
      return NIL_NODE;
    }
  }
}
