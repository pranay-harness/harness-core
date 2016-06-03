/**
 *
 */
package software.wings.common;

import software.wings.beans.Host;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.service.intfc.HostService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExpressionProcessor;

import java.util.List;
import javax.inject.Inject;

/**
 * @author Rishi
 */
public class HostExpressionProcessor implements ExpressionProcessor {
  static final String EXPRESSION_START_PATTERN = "hosts()";
  private static final String HOST_EXPR_PROCESSOR = "hostExpressionProcessor";
  @Inject private HostService hostService;
  private String[] hostNames;
  private String appId;

  public HostExpressionProcessor(ExecutionContext context) {
    // Derive appId, serviceId, serviceTemplate and tags associated from the context
  }

  @Override
  public String getPrefixObjectName() {
    return HOST_EXPR_PROCESSOR;
  }

  @Override
  public String normalizeExpression(String expression) {
    if (expression == null || !expression.startsWith(EXPRESSION_START_PATTERN)) {
      return null;
    }
    expression = HOST_EXPR_PROCESSOR + "." + expression;
    if (!expression.endsWith(".list()")) {
      expression = expression + ".list()";
    }
    return expression;
  }

  public HostExpressionProcessor hosts(String... hostNames) {
    this.hostNames = hostNames;
    return this;
  }

  public HostExpressionProcessor withNames(String... hostNames) {
    this.hostNames = hostNames;
    return this;
  }

  public List<Host> list() {
    List<Host> hosts = null;
    PageRequest<Host> pageRequest =
        PageRequest.Builder.aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter(SearchFilter.Builder.aSearchFilter().withField("appId", Operator.EQ, appId).build())
            .build();
    if (hostNames == null || hostNames.length == 0) {
      hosts = hostService.list(pageRequest);
    } else {
      // TODO :
    }

    return hosts;
  }
}
