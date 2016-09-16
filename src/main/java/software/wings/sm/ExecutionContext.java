/**
 *
 */

package software.wings.sm;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ErrorStrategy;

import java.util.List;

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
  Object evaluateExpression(String expression);

  /**
   * Evaluate expression.
   *
   * @param expression         the expression
   * @param stateExecutionData the state execution data
   * @return the object
   */
  Object evaluateExpression(String expression, StateExecutionData stateExecutionData);

  /**
   * Gets state execution data.
   *
   * @return the state execution data
   */
  StateExecutionData getStateExecutionData();

  /**
   * Render expression.
   *
   * @param expression the expression
   * @return the string
   */
  String renderExpression(String expression);

  /**
   * Render expression.
   *
   * @param expression         the expression
   * @param stateExecutionData the state execution data
   * @return the string
   */
  String renderExpression(String expression, StateExecutionData stateExecutionData);

  /**
   * Gets context element.
   *
   * @param <T>                the type parameter
   * @param contextElementType the context element type
   * @return the context element
   */
  <T extends ContextElement> T getContextElement(ContextElementType contextElementType);

  /**
   * Gets context element list.
   *
   * @param <T>                the type parameter
   * @param contextElementType the context element type
   * @return the context element list
   */
  <T extends ContextElement> List<T> getContextElementList(ContextElementType contextElementType);

  /**
   * Gets app.
   *
   * @return app app
   */
  Application getApp();

  /**
   * Gets env.
   *
   * @return env env
   */
  Environment getEnv();

  /**
   * Gets error strategy.
   *
   * @return the error strategy
   */
  ErrorStrategy getErrorStrategy();

  /**
   * Gets workflow execution id.
   *
   * @return the workflow execution id
   */
  String getWorkflowExecutionId();

  /**
   * Gets workflow execution name.
   *
   * @return the workflow execution name
   */
  String getWorkflowExecutionName();

  /**
   * Gets state execution instance id.
   *
   * @return the state execution instance id
   */
  String getStateExecutionInstanceId();

  /**
   * Gets state execution instance name.
   *
   * @return the state execution instance name
   */
  String getStateExecutionInstanceName();
}
