package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * Created by rishi on 1/12/17.
 */
public class AwsNodeProvisionState extends State {
  public AwsNodeProvisionState(String name) {
    super(name, StateType.AWS_NODE_PROVISION.name());
  }

  private String awsComputeProviderId;
  private int instanceCount;

  // TODO - more attributes whatever needed for the AWS node provision

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return null;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
