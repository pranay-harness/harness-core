package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * Created by rishi on 12/16/16.
 */
public class SubWorkflowState extends State {
  /**
   * Instantiates a new repeat state.
   *
   * @param name the name
   */
  public SubWorkflowState(String name) {
    super(name, StateType.SUB_WORKFLOW.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return null;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
