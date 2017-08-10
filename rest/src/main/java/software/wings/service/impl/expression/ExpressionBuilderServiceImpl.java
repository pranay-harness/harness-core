package software.wings.service.impl.expression;

import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.expression.ExpressionBuilderService;
import software.wings.sm.StateType;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sgurubelli on 8/7/17.
 */
@Singleton
public class ExpressionBuilderServiceImpl implements ExpressionBuilderService {
  @Inject private AppService appService;

  @Inject private ServiceExpressionBuilder serviceExpressionsBuilder;

  @Inject private EnvironmentExpressionBuilder envExpressionBuilder;

  @Override
  public List<String> listExpressions(String appId, String entityId, EntityType entityType) {
    return listExpressions(appId, entityId, entityType, null, null);
  }

  @Override
  public List<String> listExpressions(String appId, String entityId, EntityType entityType, String serviceId) {
    return listExpressions(appId, entityId, entityType, serviceId, null);
  }

  @Override
  public List<String> listExpressions(
      String appId, String entityId, EntityType entityType, String serviceId, StateType stateType) {
    Application application = appService.get(appId);
    Validator.notNullCheck("application", application);
    Validator.notNullCheck("entityId", entityId);
    List<String> expressions = new ArrayList<>();
    if (entityType.equals(EntityType.SERVICE)) {
      expressions.addAll(serviceExpressionsBuilder.getExpressions(appId, entityId));
    } else if (entityType.equals(EntityType.ENVIRONMENT)) {
      expressions.addAll(envExpressionBuilder.getExpressions(appId, entityId, serviceId));
    } else {
      return asList();
    }
    return expressions;
  }
}
