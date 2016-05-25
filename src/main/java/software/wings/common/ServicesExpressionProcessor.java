/**
 *
 */
package software.wings.common;

import com.google.common.collect.Lists;

import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExpressionProcessor;

import java.util.List;

/**
 * @author Rishi
 *
 */
public class ServicesExpressionProcessor implements ExpressionProcessor {
  private static final String SERVICES_EXPR_PROCESSOR = "servicesExpressionProcessor";
  static final String EXPRESSION_START_PATTERN = "services()";

  private ServiceResourceService serviceResourceService;
  private String appId;

  private String[] serviceNames;

  public ServicesExpressionProcessor(ExecutionContext context, ServiceResourceService serviceResourceService) {
    this.serviceResourceService = serviceResourceService;
    ExecutionContextImpl contextImpl = (ExecutionContextImpl) context;
    // retrieve appId from the context
    this.appId = contextImpl.getStateExecutionInstance().getAppId();
  }

  @Override
  public String getPrefixObjectName() {
    return SERVICES_EXPR_PROCESSOR;
  }

  @Override
  public String normalizeExpression(String expression) {
    if (expression == null || !expression.startsWith(EXPRESSION_START_PATTERN)) {
      return null;
    }
    expression = SERVICES_EXPR_PROCESSOR + "." + expression;
    if (!expression.endsWith(".list()")) {
      expression = expression + ".list()";
    }
    return expression;
  }

  public ServicesExpressionProcessor services(String... serviceNames) {
    this.serviceNames = serviceNames;
    return this;
  }
  public ServicesExpressionProcessor withNames(String... serviceNames) {
    this.serviceNames = serviceNames;
    return this;
  }

  public List<Service> list() {
    List<Service> services = null;
    PageRequest<Service> pageRequest = PageRequest.Builder.aPageRequest()
                                           .withLimit(PageRequest.UNLIMITED)
                                           .withFilters(Lists.newArrayList(SearchFilter.Builder.aSearchFilter()
                                                                               .withFieldName("appId")
                                                                               .withFieldValue(appId)
                                                                               .withOp(Operator.EQ)
                                                                               .build()))
                                           .build();
    if (serviceNames == null || serviceNames.length == 0) {
      services = serviceResourceService.list(pageRequest);
    } else {
      // TODO :
    }

    return services;
  }
}
