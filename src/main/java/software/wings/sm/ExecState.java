/**
 *
 */
package software.wings.sm;

/**
 * @author Rishi
 */
public class ExecState extends State {
  /**
   *
   */
  private static final long serialVersionUID = 1L;
  String path;
  boolean embeded;

  /**
   * @param name
   * @param stateType
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
