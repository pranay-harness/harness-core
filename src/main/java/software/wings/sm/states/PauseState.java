package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * A Pause state to pause state machine execution.
 *
 * @author Rishi
 */
public class PauseState extends State {
  private static final long serialVersionUID = 1L;

  /**
   * Creates pause state with given name.
   *
   * @param name name of the state.
   */
  public PauseState(String name) {
    super(name, StateType.PAUSE.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return null;
  }
}
