/**
 *
 */
package software.wings.sm;

import java.util.List;

/**
 * @author Rishi
 */
public interface ExpressionProcessorFactory {
  public ExpressionProcessor getExpressionProcessor(String expression, ExecutionContext context);

  public List<ExpressionProcessor> getExpressionProcessors(ExecutionContext executionContext);
}
