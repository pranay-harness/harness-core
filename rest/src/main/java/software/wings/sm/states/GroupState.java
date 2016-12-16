package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * Created by rishi on 12/16/16.
 */
public class GroupState extends State {
  /**
   * Instantiates a new repeat state.
   *
   * @param name the name
   */
  public GroupState(String name) {
    super(name, StateType.GROUP.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return null;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
