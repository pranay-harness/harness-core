/**
 *
 */

package software.wings.common;

import com.google.common.collect.Lists;
import com.google.inject.Injector;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExpressionProcessor;
import software.wings.sm.ExpressionProcessorFactory;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

// TODO: Auto-generated Javadoc

/**
 * A factory for creating WingsExpressionProcessor objects.
 */
@Singleton
public class WingsExpressionProcessorFactory implements ExpressionProcessorFactory {
  @Inject private Injector injector;

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

    return null;
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
    ArrayList<ExpressionProcessor> processorList =
        Lists.newArrayList(new ServiceExpressionProcessor(context), new HostExpressionProcessor(context));
    for (ExpressionProcessor processor : processorList) {
      injector.injectMembers(processor);
    }
    return processorList;
  }
}
