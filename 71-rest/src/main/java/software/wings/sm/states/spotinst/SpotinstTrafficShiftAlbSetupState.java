package software.wings.sm.states.spotinst;

import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
import io.harness.exception.InvalidRequestException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

@ToString
@Slf4j
public class SpotinstTrafficShiftAlbSetupState extends State {
  @Getter @Setter private String minInstancesExpr;
  @Getter @Setter private String maxInstancesExpr;
  @Getter @Setter private String targetInstancesExpr;
  @Getter @Setter private String elastiGroupNamePrefix;
  @Getter @Setter private String timeoutIntervalInMinExpr;
  @Getter @Setter private boolean useCurrentRunningCount;
  @Getter @Setter private List<LbDetailsForAlbTrafficShift> lbDetails;

  public SpotinstTrafficShiftAlbSetupState(String name) {
    super(name, StateType.SPOTINST_ALB_SHIFT_SETUP.name());
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