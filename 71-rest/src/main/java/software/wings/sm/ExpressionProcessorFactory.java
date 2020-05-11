/**
 *
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;

/**
 * A factory for creating ExpressionProcessor objects.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
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
