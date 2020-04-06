package software.wings.sm.states.spotinst;

import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.Map;

@ToString
@Slf4j
public class SpotinstTrafficShiftAlbRollbackSwitchRoutesState extends State {
  public SpotinstTrafficShiftAlbRollbackSwitchRoutesState(String name) {
    super(name, StateType.SPOTINST_LISTENER_ALB_SHIFT_ROLLBACK.name());
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
    // Do nothing on abort
  }
}