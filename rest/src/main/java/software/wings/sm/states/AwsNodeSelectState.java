package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * Created by rishi on 1/12/17.
 */
public class AwsNodeSelectState extends State {
  public AwsNodeSelectState(String name) {
    super(name, StateType.AWS_NODE_SELECT.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return null;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
