package software.wings.api.instancedetails;

import io.harness.data.SweepingOutput;
import io.harness.deployment.InstanceDetails;

import software.wings.api.InstanceElement;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InstanceInfoVariables implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "deploymentInstanceData";

  private List<InstanceDetails> instanceDetails;
  private List<InstanceElement> instanceElements;
  private Integer newInstanceTrafficPercent;
  private boolean skipVerification;

  public boolean isDeployStateInfo() {
    return newInstanceTrafficPercent == null;
  }
}
