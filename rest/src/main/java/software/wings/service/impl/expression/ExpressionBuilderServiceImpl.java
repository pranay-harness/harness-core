package software.wings.service.impl.expression;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.expression.ExpressionBuilderService;
import software.wings.sm.StateType;
import software.wings.utils.Validator;

import java.util.Set;
import java.util.TreeSet;

/**
 * Created by sgurubelli on 8/7/17.
 */
@Singleton
public class ExpressionBuilderServiceImpl implements ExpressionBuilderService {
  @Inject private AppService appService;
  @Inject private ServiceExpressionBuilder serviceExpressionsBuilder;
  @Inject private EnvironmentExpressionBuilder envExpressionBuilder;
  @Inject private WorkflowExpressionBuilder workflowExpressionBuilder;

  @Override
  public Set<String> listExpressions(String appId, String entityId, EntityType entityType) {
    return listExpressions(appId, entityId, entityType, null, null);
  }

  @Override
  public Set<String> listExpressions(String appId, String entityId, EntityType entityType, String serviceId) {
    return listExpressions(appId, entityId, entityType, serviceId, null);
  }

  @Override
  public Set<String> listExpressions(
      String appId, String entityId, EntityType entityType, String serviceId, StateType stateType) {
    Application application = appService.get(appId);
    Validator.notNullCheck("application", application);
    Validator.notNullCheck("entityId", entityId);
    Validator.notNullCheck("entityType", entityId);
    Set<String> expressions = new TreeSet<>();
    if (entityType.equals(EntityType.SERVICE)) {
      expressions.addAll(serviceExpressionsBuilder.getExpressions(appId, entityId));
    } else if (entityType.equals(EntityType.ENVIRONMENT)) {
      expressions.addAll(envExpressionBuilder.getExpressions(appId, entityId, serviceId));
    } else if (entityType.equals(EntityType.WORKFLOW)) {
      expressions.addAll(workflowExpressionBuilder.getExpressions(appId, entityId, serviceId, stateType));
    } else {
      return new TreeSet<>();
    }
    return expressions;
  }
}
