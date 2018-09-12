package software.wings.service.impl.expression;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.EntityType.APPLICATION;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.expression.ExpressionBuilderService;
import software.wings.sm.StateType;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Created by sgurubelli on 8/7/17.
 */
@Singleton
public class ExpressionBuilderServiceImpl implements ExpressionBuilderService {
  @Inject private AccountService accountService;
  @Inject private AppService appService;
  @Inject private ServiceExpressionBuilder serviceExpressionsBuilder;
  @Inject private EnvironmentExpressionBuilder envExpressionBuilder;
  @Inject private WorkflowExpressionBuilder workflowExpressionBuilder;
  @Inject private ApplicationExpressionBuilder applicationExpressionBuilder;

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
    Set<String> expressions = new TreeSet<>();
    Application application = appService.getApplicationWithDefaults(appId);
    notNullCheck("Application does not exist. May be deleted", application, USER);
    notNullCheck("EntityId is mandatory", entityId, USER);
    notNullCheck("EntityType is mandatory", entityType, USER);
    Map<String, String> defaults = application.getDefaults();
    if (defaults != null) {
      expressions.addAll(defaults.keySet().stream().map(s -> "app.defaults." + s).collect(Collectors.toSet()));
    }
    Account account = accountService.getAccountWithDefaults(application.getAccountId());
    if (account != null && isNotEmpty(account.getDefaults())) {
      expressions.addAll(
          account.getDefaults().keySet().stream().map(s -> "account.defaults." + s).collect(Collectors.toSet()));
    }
    if (entityType.equals(SERVICE)) {
      expressions.addAll(serviceExpressionsBuilder.getExpressions(appId, entityId, stateType));
    } else if (entityType.equals(ENVIRONMENT)) {
      expressions.addAll(envExpressionBuilder.getExpressions(appId, entityId, serviceId));
    } else if (entityType.equals(WORKFLOW)) {
      expressions.addAll(workflowExpressionBuilder.getExpressions(appId, entityId, serviceId, stateType));
    } else if (entityType.equals(APPLICATION)) {
      expressions.addAll(applicationExpressionBuilder.getExpressions(appId, entityId));
    }
    return expressions;
  }
}
