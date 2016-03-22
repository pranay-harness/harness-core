/**
 *
 */
package software.wings.sm;

/**
 * @author Rishi
 *
 */
public class StateA extends State {
  public StateA() {
    super(StateA.class.getName(), StateType.HTTP);
  }
  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    System.out.println("Executing ..." + getClass());
    context.setParam("StateA", StateA.class.getName());
    System.out.println("context params:" + context.getParams());
    return new ExecutionResponse();
  }
}
