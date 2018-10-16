package software.wings.service.impl.expression;

import static java.util.Arrays.asList;

import java.util.Set;
import java.util.TreeSet;

public class ApplicationExpressionBuilder extends ExpressionBuilder {
  @Override
  public Set<String> getExpressions(String appId, String entityId) {
    Set<String> expressions = new TreeSet<>();
    expressions.addAll(asList(APP_NAME, APP_DESCRIPTION));
    expressions.addAll(asList(ENV_NAME, ENV_DESCRIPTION));
    expressions.addAll(asList(SERVICE_NAME, SERVICE_DESCRIPTION));
    expressions.addAll(asList(WORKFLOW_NAME, WORKFLOW_DESCRIPTION));
    expressions.addAll(asList(PIPELINE_NAME, PIPELINE_DESCRIPTION));
    return expressions;
  }

  @Override
  public Set<String> getDynamicExpressions(String appId, String entityId) {
    return null;
  }
}
