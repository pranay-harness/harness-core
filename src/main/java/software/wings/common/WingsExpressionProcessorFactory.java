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

@Singleton
public class WingsExpressionProcessorFactory implements ExpressionProcessorFactory {
  @Inject private Injector injector;

  @Override
  public ExpressionProcessor getExpressionProcessor(String expression, ExecutionContext context) {
    ExpressionProcessor processor = null;
    if (expression.startsWith(ServiceExpressionProcessor.EXPRESSION_START_PATTERN)) {
      processor = new ServiceExpressionProcessor(context);
    } else if (expression.startsWith(HostExpressionProcessor.EXPRESSION_START_PATTERN)) {
      processor = new HostExpressionProcessor(context);
    } else if (expression.startsWith(InstanceExpressionProcessor.EXPRESSION_START_PATTERN)) {
      processor = new InstanceExpressionProcessor(context);
    }
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
