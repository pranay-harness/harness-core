/**
 *
 */
package software.wings.sm;

/**
 * @author Rishi
 *
 */
public class PauseState extends State {
  /**
   * @param name
   * @param stateType
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

  private static final long serialVersionUID = 1L;
}
