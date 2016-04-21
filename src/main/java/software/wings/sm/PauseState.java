package software.wings.sm;

/**
 * A Pause state to pause state machine execution.
 * @author Rishi
 */
public class PauseState extends State {
  private static final long serialVersionUID = 1L;

  /**
   * Creates pause state with given name.
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
