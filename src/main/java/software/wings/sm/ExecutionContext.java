/**
 *
 */

package software.wings.sm;

// TODO: Auto-generated Javadoc

/**
 * The Interface ExecutionContext.
 *
 * @author Rishi
 */
public interface ExecutionContext {
  /**
   * Evaluate expression.
   *
   * @param expression the expression
   * @return the object
   */
  public Object evaluateExpression(String expression);

  /**
   * Evaluate expression.
   *
   * @param expression         the expression
   * @param stateExecutionData the state execution data
   * @return the object
   */
  public Object evaluateExpression(String expression, StateExecutionData stateExecutionData);

  /**
   * Gets state execution data.
   *
   * @return the state execution data
   */
  public StateExecutionData getStateExecutionData();

  /**
   * Render expression.
   *
   * @param expression the expression
   * @return the string
   */
  public String renderExpression(String expression);

  /**
   * Render expression.
   *
   * @param expression         the expression
   * @param stateExecutionData the state execution data
   * @return the string
   */
  public String renderExpression(String expression, StateExecutionData stateExecutionData);
}
