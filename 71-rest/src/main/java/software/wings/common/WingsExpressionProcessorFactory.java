/**
 *
 */

package software.wings.common;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExpressionProcessor;
import software.wings.sm.ExpressionProcessorFactory;

import java.util.List;

/**
 * A factory for creating WingsExpressionProcessor objects.
 */
@Singleton
public class WingsExpressionProcessorFactory implements ExpressionProcessorFactory {
  @Inject private Injector injector;

  /**
   * Gets matching expression processor.
   *
   * @param expression the expression
   * @param context    the context
   * @return the matching expression processor
   */
  public static ExpressionProcessor getMatchingExpressionProcessor(String expression, ExecutionContext context) {
    ExpressionProcessor processor = new ServiceExpressionProcessor(context);
    if (processor.matches(expression)) {
      return processor;
    }

    processor = new HostExpressionProcessor(context);
    if (processor.matches(expression)) {
      return processor;
    }
    processor = new InstanceExpressionProcessor(context);
    if (processor.matches(expression)) {
      return processor;
    }
    processor = new InstancePartitionExpressionProcessor(context);
    if (processor.matches(expression)) {
      return processor;
    }

    processor = new AwsLambdaFunctionProcessor(context);
    if (processor.matches(expression)) {
      return processor;
    }

    return null;
  }

  /**
   * Gets default expression.
   *
   * @param contextElementType the context element type
   * @return the default expression
   */
  public static String getDefaultExpression(ContextElementType contextElementType) {
    switch (contextElementType) {
      case SERVICE:
        return ServiceExpressionProcessor.DEFAULT_EXPRESSION;
      case HOST:
        return HostExpressionProcessor.DEFAULT_EXPRESSION;
      case INSTANCE:
        return InstanceExpressionProcessor.DEFAULT_EXPRESSION;
      case AWS_LAMBDA_FUNCTION:
        return AwsLambdaFunctionProcessor.DEFAULT_EXPRESSION;
      default:
        return "";
    }
  }

  @Override
  public ExpressionProcessor getExpressionProcessor(String expression, ExecutionContext context) {
    ExpressionProcessor processor = getMatchingExpressionProcessor(expression, context);
    if (processor != null) {
      injector.injectMembers(processor);
    }
    return processor;
  }

  @Override
  public List<ExpressionProcessor> getExpressionProcessors(ExecutionContext context) {
    List<ExpressionProcessor> processorList =
        Lists.newArrayList(new ServiceExpressionProcessor(context), new HostExpressionProcessor(context));
    for (ExpressionProcessor processor : processorList) {
      injector.injectMembers(processor);
    }
    return processorList;
  }
}
