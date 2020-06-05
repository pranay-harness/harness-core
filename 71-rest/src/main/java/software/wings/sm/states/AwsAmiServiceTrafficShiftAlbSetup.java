package software.wings.sm.states;

import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
import io.harness.exception.InvalidRequestException;
import lombok.Getter;
import lombok.Setter;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

public class AwsAmiServiceTrafficShiftAlbSetup extends State {
  @Getter @Setter private String autoScalingGroupName;
  @Getter @Setter private String autoScalingSteadyStateTimeout;
  @Getter @Setter private boolean useCurrentRunningCount;
  @Getter @Setter private String maxInstances;
  @Getter @Setter private String minInstances;
  @Getter @Setter private String desiredInstances;
  @Getter @Setter private List<LbDetailsForAlbTrafficShift> lbDetails;

  public AwsAmiServiceTrafficShiftAlbSetup(String name) {
    super(name, StateType.ASG_AMI_SERVICE_ALB_SHIFT_SETUP.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    throw new InvalidRequestException("Not implemented yet");
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    throw new InvalidRequestException("Not implemented yet");
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    throw new InvalidRequestException("Not implemented yet");
  }
}
