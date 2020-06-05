package software.wings.sm.states;

import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.Map;

public class AwsAmiRollbackTrafficShiftAlbSwitchRoutesState extends State {
  public AwsAmiRollbackTrafficShiftAlbSwitchRoutesState(String name) {
    super(name, StateType.ASG_AMI_ROLLBACK_ALB_SHIFT_SWITCH_ROUTES.name());
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
