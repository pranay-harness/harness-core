package software.wings.api.instancedetails;

import io.harness.beans.SweepingOutput;
import io.harness.deployment.InstanceDetails;
import lombok.Builder;
import lombok.Value;
import software.wings.api.InstanceElement;

import java.util.List;

@Value
@Builder
public class InstanceInfoVariables implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "deploymentInstanceData";

  private List<InstanceDetails> instanceDetails;
  private List<InstanceElement> instanceElements;
  private Integer newInstanceTrafficPercent;

  public boolean isDeployStateInfo() {
    return newInstanceTrafficPercent == null;
  }
}
