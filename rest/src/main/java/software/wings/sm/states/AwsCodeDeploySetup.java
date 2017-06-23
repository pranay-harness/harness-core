package software.wings.sm.states;

import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * Created by brett on 6/22/17
 */
public class AwsCodeDeploySetup extends State {
  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public AwsCodeDeploySetup(String name) {
    super(name, StateType.AWS_CD_SETUP.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return anExecutionResponse().withExecutionStatus(ExecutionStatus.SUCCESS).build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
