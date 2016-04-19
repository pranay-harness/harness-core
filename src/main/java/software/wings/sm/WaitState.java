/**
 *
 */
package software.wings.sm;

/**
 * @author Rishi
 *
 */
public class WaitState extends State {
  /**
   * @param name
   * @param stateType
   */
  public WaitState(String name) {
    super(name, StateType.WAIT.name());
  }

  private static final long serialVersionUID = 1L;

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    // TODO Auto-generated method stub
    return null;
  }
}
