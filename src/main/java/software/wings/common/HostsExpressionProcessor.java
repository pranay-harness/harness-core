/**
 *
 */
package software.wings.common;

import com.google.common.collect.Lists;

import software.wings.beans.Host;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.service.intfc.HostService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExpressionProcessor;

import java.util.List;

/**
 * @author Rishi
 *
 */
public class HostsExpressionProcessor implements ExpressionProcessor {
  private static final String HOSTS_EXPR_PROCESSOR = "hostsExpressionProcessor";
  static final String EXPRESSION_START_PATTERN = "hosts()";

  private HostService hostService;
  private String[] hostNames;
  private Object appId;

  public HostsExpressionProcessor(ExecutionContext context, HostService hostService) {
    this.hostService = hostService;
    // Derive appId, serviceId, serviceTemplate and tags associated from the context
  }

  @Override
  public String getPrefixObjectName() {
    return HOSTS_EXPR_PROCESSOR;
  }

  @Override
  public String normalizeExpression(String expression) {
    if (expression == null || !expression.startsWith(EXPRESSION_START_PATTERN)) {
      return null;
    }
    expression = HOSTS_EXPR_PROCESSOR + "." + expression;
    if (!expression.endsWith(".list()")) {
      expression = expression + ".list()";
    }
    return expression;
  }

  public HostsExpressionProcessor hosts(String... hostNames) {
    this.hostNames = hostNames;
    return this;
  }
  public HostsExpressionProcessor withNames(String... hostNames) {
    this.hostNames = hostNames;
    return this;
  }

  public List<Host> list() {
    List<Host> hosts = null;
    PageRequest<Host> pageRequest = PageRequest.Builder.aPageRequest()
                                        .withLimit(PageRequest.UNLIMITED)
                                        .withFilters(Lists.newArrayList(SearchFilter.Builder.aSearchFilter()
                                                                            .withFieldName("appId")
                                                                            .withFieldValue(appId)
                                                                            .withOp(Operator.EQ)
                                                                            .build()))
                                        .build();
    if (hostNames == null || hostNames.length == 0) {
      hosts = hostService.list(pageRequest);
    } else {
      // TODO :
    }

    return hosts;
  }
}
