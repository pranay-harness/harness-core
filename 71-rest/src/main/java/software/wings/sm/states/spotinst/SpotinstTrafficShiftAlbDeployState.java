package software.wings.sm.states.spotinst;

import static software.wings.beans.InstanceUnitType.PERCENTAGE;

import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.InstanceUnitType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.Map;

@ToString
@Slf4j
public class SpotinstTrafficShiftAlbDeployState extends State {
  @Getter @Setter private InstanceUnitType instanceUnitType = PERCENTAGE;
  @Getter @Setter private String instanceCountExpr = "100";

  public SpotinstTrafficShiftAlbDeployState(String name) {
    super(name, StateType.SPOTINST_ALB_SHIFT_DEPLOY.name());
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