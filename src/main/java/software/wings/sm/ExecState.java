/**
 *
 */
package software.wings.sm;

/**
 * @author Rishi
 *
 */
public class ExecState extends State {
  /**
   * @param name
   * @param stateType
   */
  public ExecState(String name, StateType stateType) {
    super(name, stateType);
    // TODO Auto-generated constructor stub
  }

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  String path;
  boolean embeded;

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    // TODO Auto-generated method stub
    return null;
  }
}
