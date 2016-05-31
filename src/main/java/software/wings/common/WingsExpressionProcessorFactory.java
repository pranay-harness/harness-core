/**
 *
 */
package software.wings.common;

import com.google.common.collect.Lists;

import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExpressionProcessor;
import software.wings.sm.ExpressionProcessorFactory;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WingsExpressionProcessorFactory implements ExpressionProcessorFactory {
  @Inject private ServiceResourceService serviceResourceService;

  @Inject private HostService hostService;

  @Override
  public ExpressionProcessor getExpressionProcessor(String expression, ExecutionContext context) {
    if (expression.startsWith(ServiceExpressionProcessor.EXPRESSION_START_PATTERN)) {
      return new ServiceExpressionProcessor(context, serviceResourceService);
    } else if (expression.startsWith(HostExpressionProcessor.EXPRESSION_START_PATTERN)) {
      return new HostExpressionProcessor(context, hostService);
    }
    return null;
  }

  @Override
  public List<ExpressionProcessor> getExpressionProcessors(ExecutionContext context) {
    return Lists.newArrayList(new ServiceExpressionProcessor(context, serviceResourceService),
        new HostExpressionProcessor(context, hostService));
  }
}
