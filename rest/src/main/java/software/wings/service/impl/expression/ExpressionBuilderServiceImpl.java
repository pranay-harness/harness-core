package software.wings.service.impl.expression;

import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

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
@Singleton
public class ExpressionBuilderServiceImpl implements ExpressionBuilderService {
  @Inject private AppService appService;

  @Inject private ServiceExpressionBuilder serviceExpressionsBuilder;

  @Override
  public List<String> listExpressions(String appId, String entityId, EntityType entityType) {
    Application application = appService.get(appId);
    Validator.notNullCheck("application", application);
    Validator.notNullCheck("entityId", entityId);
    List<String> expressions = new ArrayList<>();
    if (entityType.equals(EntityType.SERVICE)) {
      expressions.addAll(serviceExpressionsBuilder.getExpressions(appId, entityId));
    } else {
      return asList();
    }
    return expressions;
  }
}
