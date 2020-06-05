package software.wings.sm.states;

import static software.wings.beans.InstanceUnitType.PERCENTAGE;

import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import lombok.Getter;
import lombok.Setter;
import software.wings.beans.InstanceUnitType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.Map;

public class AwsAmiServiceTrafficShiftAlbDeployState extends State {
  @Getter @Setter private InstanceUnitType instanceUnitType = PERCENTAGE;
  @Getter @Setter private String instanceCountExpr = "100";

  public AwsAmiServiceTrafficShiftAlbDeployState(String name) {
    super(name, StateType.ASG_AMI_SERVICE_ALB_SHIFT_DEPLOY.name());
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    throw new InvalidRequestException("Not implemented yet.");
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    throw new InvalidRequestException("Not implemented yet.");
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    throw new InvalidRequestException("Not implemented yet.");
  }
}
