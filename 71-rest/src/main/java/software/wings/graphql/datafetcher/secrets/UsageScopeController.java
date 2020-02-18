package software.wings.graphql.datafetcher.secrets;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import software.wings.graphql.datafetcher.application.AppFilterController;
import software.wings.graphql.datafetcher.environment.EnvFilterController;
import software.wings.graphql.schema.type.QLGenericFilterType;
import software.wings.graphql.schema.type.secrets.QLAppEnvScope;
import software.wings.graphql.schema.type.secrets.QLAppScopeFilter;
import software.wings.graphql.schema.type.secrets.QLEnvScopeFilter;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;

import java.util.HashSet;
import java.util.Set;

@Singleton
public class UsageScopeController {
  @Inject EnvFilterController envFilterController;
  @Inject AppFilterController appFilterController;

  public UsageRestrictions populateUsageRestrictions(QLUsageScope usageScopes, String accountId) {
    if (usageScopes == null) {
      return null;
    }
    Set<QLAppEnvScope> restrictions = usageScopes.getAppEnvScopes();
    if (isEmpty(restrictions)) {
      return UsageRestrictions.builder().appEnvRestrictions(null).build();
    }
    Set<AppEnvRestriction> usageRestrictions = new HashSet<>();
    for (QLAppEnvScope scope : restrictions) {
      QLAppScopeFilter application = scope.getApplication();
      QLEnvScopeFilter environment = scope.getEnvironment();
      appFilterController.validateAppScopeFilter(application, accountId);
      if (application.getFilterType() == QLGenericFilterType.ALL && environment.getEnvId() != null) {
        throw new InvalidRequestException("EnvId cannot be supplied with app filterType ALL");
      }
      envFilterController.validateEnvFilter(environment, accountId, application.getAppId());
      AppEnvRestriction appEnvRestriction = AppEnvRestriction.builder()
                                                .appFilter(appFilterController.createGenericEntityFilter(application))
                                                .envFilter(envFilterController.createEnvFilter(environment))
                                                .build();
      usageRestrictions.add(appEnvRestriction);
    }
    return UsageRestrictions.builder().appEnvRestrictions(usageRestrictions).build();
  }

  public QLUsageScope populateUsageScope(UsageRestrictions usageRestrictions) {
    if (usageRestrictions == null) {
      return null;
    }
    Set<AppEnvRestriction> appEnvRestrictions = usageRestrictions.getAppEnvRestrictions();
    if (isEmpty(appEnvRestrictions)) {
      return null;
    }
    Set<QLAppEnvScope> usageScope = new HashSet<>();
    for (AppEnvRestriction restriction : appEnvRestrictions) {
      GenericEntityFilter applications = restriction.getAppFilter();
      EnvFilter environments = restriction.getEnvFilter();
      QLAppEnvScope appEnvScope = QLAppEnvScope.builder()
                                      .application(appFilterController.createAppScopeFilterOutput(applications))
                                      .environment(envFilterController.createEnvScopeFilterOutput(environments))
                                      .build();
      usageScope.add(appEnvScope);
    }
    return QLUsageScope.builder().appEnvScopes(usageScope).build();
  }
}
