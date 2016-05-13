package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

/**
 * A Pause state to pause state machine execution.
 *
 * @author Rishi
 */
public class BuildState extends State {
  private static final long serialVersionUID = 1L;

  /**
   * Creates pause state with given name.
   *
   * @param name name of the state.
   */
  public BuildState(String name) {
    super(name, StateType.BUILD.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    Misc.quietSleep(2000);
    return new ExecutionResponse();
  }
}
