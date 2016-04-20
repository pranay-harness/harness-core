/**
 *
 */
package software.wings.sm;

/**
 * @author Rishi
 */
public class HttpState extends State {
  private static final long serialVersionUID = 1L;

  /**
   * @param name
   * @param stateType
   */
  public HttpState(String name) {
    super(name, StateType.HTTP.name());
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
