/**
 *
 */

package software.wings.sm;

import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * A factory for creating ExpressionProcessor objects.
 *
 * @author Rishi
 */
public interface ExpressionProcessorFactory {
  /**
   * Gets the expression processor.
   *
   * @param expression the expression
   * @param context    the context
   * @return the expression processor
   */
  public ExpressionProcessor getExpressionProcessor(String expression, ExecutionContext context);

  /**
   * Gets the expression processors.
   *
   * @param executionContext the execution context
   * @return the expression processors
   */
  public List<ExpressionProcessor> getExpressionProcessors(ExecutionContext executionContext);
}
