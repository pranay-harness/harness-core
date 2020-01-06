package software.wings.service.impl.expression;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.EntityType.APPLICATION;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.WORKFLOW;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.SubEntityType;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.expression.ExpressionBuilderService;
import software.wings.sm.StateType;
import software.wings.utils.ApplicationManifestUtils;

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
  @Inject private ApplicationManifestUtils applicationManifestUtils;

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
    return listExpressions(appId, entityId, entityType, serviceId, stateType, null);
  }

  @Override
  public Set<String> listExpressions(String appId, String entityId, EntityType entityType, String serviceId,
      StateType stateType, SubEntityType subEntityType) {
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
    if (entityType == SERVICE) {
      expressions.addAll(serviceExpressionsBuilder.getExpressions(appId, entityId, stateType));
    } else if (entityType == ENVIRONMENT) {
      expressions.addAll(envExpressionBuilder.getExpressions(appId, entityId, serviceId));
    } else if (entityType == WORKFLOW) {
      expressions.addAll(
          workflowExpressionBuilder.getExpressions(appId, entityId, serviceId, stateType, subEntityType));
      if (SubEntityType.NOTIFICATION_GROUP == subEntityType) {
        // Filter account and app defaults
        final Set<String> filteredExpressions =
            expressions.stream()
                .filter(s -> !(s.startsWith("app.defaults") || s.startsWith("account.defaults")))
                .collect(Collectors.toSet());
        expressions = new TreeSet<>(filteredExpressions);
      }
    } else if (entityType == APPLICATION) {
      expressions.addAll(applicationExpressionBuilder.getExpressions(appId, entityId));
    }
    return expressions;
  }

  @Override
  public Set<String> listExpressionsFromValuesForService(String appId, String serviceId) {
    notNullCheck("ServiceId is mandatory", serviceId, USER);

    return applicationManifestUtils.listExpressionsFromValuesForService(appId, serviceId);
  }
}
