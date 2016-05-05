/**
 *
 */
package software.wings.sm;

/**
 * @author Rishi
 *
 */
public interface ExecutionContext {
  public Object evaluateExpression(String expression);

  public Object evaluateExpression(String expression, StateExecutionData stateExecutionData);

  public <T> T evaluateExpression(String expression, Class<T> cls);

  public <T> T evaluateExpression(String expression, Class<T> cls, StateExecutionData stateExecutionData);

  public StateExecutionData getStateExecutionData();

  public String renderExpression(String expression);

  public String renderExpression(String expression, StateExecutionData stateExecutionData);
}
