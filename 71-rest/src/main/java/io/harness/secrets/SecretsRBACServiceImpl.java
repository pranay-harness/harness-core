package io.harness.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SECRETS;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secrets.setupusage.SecretSetupUsageService;
import software.wings.beans.Base;
import software.wings.beans.User;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.PermissionAttribute;
import software.wings.security.ScopedEntity;
import software.wings.security.UsageRestrictions;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserService;
import software.wings.settings.RestrictionsAndAppEnvMap;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@OwnedBy(PL)
public class SecretsRBACServiceImpl implements SecretsRBACService {
  private final UsageRestrictionsService usageRestrictionsService;
  private final SecretSetupUsageService secretSetupUsageService;
  private final UserService userService;
  private final AppService appService;
  private final EnvironmentService envService;

  @Inject
  public SecretsRBACServiceImpl(UsageRestrictionsService usageRestrictionsService,
      SecretSetupUsageService secretSetupUsageService, UserService userService, AppService appService,
      EnvironmentService environmentService) {
    this.usageRestrictionsService = usageRestrictionsService;
    this.secretSetupUsageService = secretSetupUsageService;
    this.userService = userService;
    this.appService = appService;
    this.envService = environmentService;
  }

  private void checkIfUsagesAreInScope(String accountId, String secretId, UsageRestrictions usageRestrictions) {
    Map<String, Set<String>> setupAppEnvMap = secretSetupUsageService.getUsagesAppEnvMap(accountId, secretId);
    if (setupAppEnvMap.size() == 0) {
      // This secret is not referred by any setup entities. no need to check.
      return;
    }
    usageRestrictionsService.validateSetupUsagesOnUsageRestrictionsUpdate(accountId, setupAppEnvMap, usageRestrictions);
  }

  @Override
  public boolean hasAccessToEditSecret(String accountId, ScopedEntity scopedEntity) {
    return usageRestrictionsService.userHasPermissionsToChangeEntity(
        accountId, MANAGE_SECRETS, scopedEntity.getUsageRestrictions(), scopedEntity.isScopedToAccount());
  }

  @Override
  public boolean hasAccessToReadSecrets(
      String accountId, List<ScopedEntity> scopedEntities, String appId, String envId) {
    for (ScopedEntity scopedEntity : scopedEntities) {
      if (!hasAccessToReadSecret(accountId, scopedEntity, appId, envId)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean hasAccessToReadSecret(String accountId, ScopedEntity scopedEntity) {
    return hasAccessToReadSecretInternal(accountId, scopedEntity, null, null);
  }

  @Override
  public boolean hasAccessToReadSecret(String accountId, ScopedEntity scopedEntity, String appId, String envId) {
    return hasAccessToReadSecretInternal(accountId, scopedEntity, appId, envId);
  }

  @Override
  public void canReplacePermissions(String accountId, ScopedEntity newScopedEntity, ScopedEntity oldScopedEntity,
      String secretId, boolean validateReferences) {
    usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(accountId, MANAGE_SECRETS,
        oldScopedEntity.getUsageRestrictions(), newScopedEntity.getUsageRestrictions(),
        newScopedEntity.isScopedToAccount());
    if (!Objects.equals(oldScopedEntity.getUsageRestrictions(), newScopedEntity.getUsageRestrictions())
        && validateReferences) {
      // Validate if change of the usage scope is resulting in with dangling references in service/environments.
      checkIfUsagesAreInScope(accountId, secretId, newScopedEntity.getUsageRestrictions());
    }
  }

  @Override
  public void canSetPermissions(String accountId, ScopedEntity scopedEntity) {
    usageRestrictionsService.validateUsageRestrictionsOnEntitySave(
        accountId, MANAGE_SECRETS, scopedEntity.getUsageRestrictions(), scopedEntity.isScopedToAccount());
  }

  @Override
  public UsageRestrictions getDefaultUsageRestrictions(String accountId) {
    if (hasUserContext()) {
      return usageRestrictionsService.getDefaultUsageRestrictions(accountId, null, null);
    } else {
      GenericEntityFilter appFilter =
          GenericEntityFilter.builder().filterType(GenericEntityFilter.FilterType.ALL).build();
      EnvFilter envFilter = EnvFilter.builder().filterTypes(Sets.newHashSet(PROD, NON_PROD)).build();
      UsageRestrictions.AppEnvRestriction appEnvRestriction =
          UsageRestrictions.AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      UsageRestrictions usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(Sets.newHashSet(appEnvRestriction));
      return usageRestrictions;
    }
  }

  private boolean hasAccessToReadSecretInternal(
      String accountId, ScopedEntity scopedEntity, String appId, String envId) {
    boolean isAccountAdmin = userService.hasPermission(accountId, MANAGE_SECRETS);
    RestrictionsAndAppEnvMap restrictionsAndAppEnvMap =
        usageRestrictionsService.getRestrictionsAndAppEnvMapFromCache(accountId, PermissionAttribute.Action.READ);
    Map<String, Set<String>> appEnvMapFromPermissions = restrictionsAndAppEnvMap.getAppEnvMap();
    UsageRestrictions restrictionsFromUserPermissions = restrictionsAndAppEnvMap.getUsageRestrictions();

    Set<String> appsByAccountId = appService.getAppIdsAsSetByAccountId(accountId);
    Map<String, List<Base>> appIdEnvMapForAccount = envService.getAppIdEnvMap(appsByAccountId);

    return usageRestrictionsService.hasAccess(accountId, isAccountAdmin, appId, envId,
        scopedEntity.getUsageRestrictions(), restrictionsFromUserPermissions, appEnvMapFromPermissions,
        appIdEnvMapForAccount, scopedEntity.isScopedToAccount());
  }

  private boolean hasUserContext() {
    User user = UserThreadLocal.get();
    return user != null && user.getUserRequestContext() != null;
  }
}