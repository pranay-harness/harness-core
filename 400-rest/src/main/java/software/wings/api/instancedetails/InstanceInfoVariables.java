package software.wings.api.instancedetails;

import io.harness.beans.SweepingOutput;
import io.harness.deployment.InstanceDetails;

import software.wings.api.InstanceElement;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("instanceInfoVariables")
public class InstanceInfoVariables implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "deploymentInstanceData";

  private List<InstanceDetails> instanceDetails;
  private List<InstanceElement> instanceElements;
  private Integer newInstanceTrafficPercent;
  private boolean skipVerification;

  public boolean isDeployStateInfo() {
    return newInstanceTrafficPercent == null;
  }

  @Override
  public String getType() {
    return "instanceInfoVariables";
  }
}
