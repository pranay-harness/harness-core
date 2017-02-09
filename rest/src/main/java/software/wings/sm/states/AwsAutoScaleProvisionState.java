package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * Created by rishi on 1/12/17.
 */
public class AwsAutoScaleProvisionState extends State {
  public AwsAutoScaleProvisionState(String name) {
    super(name, StateType.AWS_AUTOSCALE_PROVISION.name());
  }

  private String awsComputeProviderId;
  private String autoScaleGroupNamePattern;
  private String launchConfigurationName;
  // TODO - more relevant attribute

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return null;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
