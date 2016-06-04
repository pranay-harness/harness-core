package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

// TODO: Auto-generated Javadoc

/**
 * Execution type state.
 *
 * @author Rishi
 */
public class ExecState extends State {
  private static final long serialVersionUID = 1L;
  String path;
  boolean embeded;

  /**
   * Creates a new Exec State.
   *
   * @param name      state name.
   * @param stateType Type of state.
   */
  public ExecState(String name, StateType stateType) {
    super(name, stateType.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    // TODO Auto-generated method stub
    return null;
  }
}
