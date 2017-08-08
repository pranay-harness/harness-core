package software.wings.service.impl.expression;

import static java.util.Arrays.asList;
import static software.wings.service.impl.expression.ServiceExpressions.Builder.*;

import com.google.inject.Inject;

import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.expression.ExpressionBuilderService;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sgurubelli on 8/7/17.
 */
public class ExpressionBuilderServiceImpl implements ExpressionBuilderService {
  @Inject private AppService appService;

  @Override
  public List<String> listExpressions(String appId, String entityId, EntityType entityType) {
    Application application = appService.get(appId);
    Validator.notNullCheck("application", application);
    Validator.notNullCheck("entityId", entityId);
    List<String> expressions = new ArrayList<>();
    ExpressionBuilder expressionBuilder = null;
    if (entityType.equals(EntityType.SERVICE)) {
      expressionBuilder = aServiceExpressions().withAppId(appId).withEntityId(entityId).build();
    } else {
      return asList();
    }

    expressions.addAll(expressionBuilder.getExpressions());
    expressions.addAll(expressionBuilder.getDynamicExpressions());
    return expressions;
  }
}
