/**
 *
 */
package software.wings.sm;

import java.util.List;

/**
 * @author Rishi
 */
public class DefaultExpressionProcessorFactory implements ExpressionProcessorFactory {
  @Override
  public ExpressionProcessor getExpressionProcessor(String expression, ExecutionContext context) {
    return null;
  }

  @Override
  public List<ExpressionProcessor> getExpressionProcessors(ExecutionContext executionContext) {
    return null;
  }
}
