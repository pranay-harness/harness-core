/**
 *
 */
package software.wings.sm;

/**
 * @author Rishi
 *
 */
public class StateC extends State {
  public StateC() {
    super(StateC.class.getName(), StateType.HTTP);
  }
  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    context.setParam("StateC", StateC.class.getName());
    System.out.println("Executing ..." + getClass());
    System.out.println("context params:" + context.getParams());
    return new ExecutionResponse();
  }
}
