package software.wings.sm;

/**
 * Http state which makes a call to http service.
 * @author Rishi
 */
public class HttpState extends State {
  private static final long serialVersionUID = 1L;

  /**
   * Create a new Http State with given name.
   * @param name name of the state.
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
