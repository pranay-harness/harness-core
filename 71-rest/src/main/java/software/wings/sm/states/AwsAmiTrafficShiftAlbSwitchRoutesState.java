package software.wings.sm.states;

import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import lombok.Getter;
import lombok.Setter;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.Map;

public class AwsAmiTrafficShiftAlbSwitchRoutesState extends State {
  @Getter @Setter private boolean downsizeOldAutoScalingGroup;
  @Getter @Setter private String newAutoScalingGroupWeightExpr;

  public AwsAmiTrafficShiftAlbSwitchRoutesState(String name) {
    super(name, StateType.ASG_AMI_ALB_SHIFT_SWITCH_ROUTES.name());
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
