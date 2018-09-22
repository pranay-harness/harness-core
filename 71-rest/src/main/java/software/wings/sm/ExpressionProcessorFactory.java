/**
 *
 */

package software.wings.sm;

import java.util.List;

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
  ExpressionProcessor getExpressionProcessor(String expression, ExecutionContext context);

  /**
   * Gets the expression processors.
   *
   * @param executionContext the execution context
   * @return the expression processors
   */
  List<ExpressionProcessor> getExpressionProcessors(ExecutionContext executionContext);
}
