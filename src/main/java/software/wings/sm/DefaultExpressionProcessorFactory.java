/**
 *
 */

package software.wings.sm;

import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * A factory for creating DefaultExpressionProcessor objects.
 *
 * @author Rishi
 */
public class DefaultExpressionProcessorFactory implements ExpressionProcessorFactory {
  /* (non-Javadoc)
   * @see software.wings.sm.ExpressionProcessorFactory#getExpressionProcessor(java.lang.String,
   * software.wings.sm.ExecutionContext)
   */
  @Override
  public ExpressionProcessor getExpressionProcessor(String expression, ExecutionContext context) {
    return null;
  }

  /* (non-Javadoc)
   * @see software.wings.sm.ExpressionProcessorFactory#getExpressionProcessors(software.wings.sm.ExecutionContext)
   */
  @Override
  public List<ExpressionProcessor> getExpressionProcessors(ExecutionContext executionContext) {
    return null;
  }
}
