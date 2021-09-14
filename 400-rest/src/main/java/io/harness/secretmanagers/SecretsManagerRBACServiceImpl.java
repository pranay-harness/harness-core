/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secretmanagers;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SECRET_MANAGERS;

import software.wings.beans.Base;
import software.wings.security.PermissionAttribute;
import software.wings.security.ScopedEntity;
import software.wings.security.UsageRestrictions;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserService;
import software.wings.settings.RestrictionsAndAppEnvMap;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SecretsManagerRBACServiceImpl implements SecretsManagerRBACService {
  private final UsageRestrictionsService usageRestrictionsService;
  private final UserService userService;
  private final AppService appService;
  private final EnvironmentService envService;

  @Inject
  public SecretsManagerRBACServiceImpl(UsageRestrictionsService usageRestrictionsService, UserService userService,
      AppService appService, EnvironmentService environmentService) {
    this.usageRestrictionsService = usageRestrictionsService;
    this.userService = userService;
    this.appService = appService;
    this.envService = environmentService;
  }

  @Override
  public boolean hasAccessToEditSM(String accountId, ScopedEntity scopedEntity) {
    return usageRestrictionsService.userHasPermissionsToChangeEntity(
        accountId, MANAGE_SECRET_MANAGERS, scopedEntity.getUsageRestrictions(), scopedEntity.isScopedToAccount());
  }

  @Override
  public boolean hasAccessToReadSM(String accountId, ScopedEntity scopedEntity, String appId, String envId) {
    return hasAccessToReadSMInternal(accountId, scopedEntity, appId, envId);
  }

  @Override
  public void canChangePermissions(String accountId, ScopedEntity newScopedEntity, ScopedEntity oldScopedEntity) {
    usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(accountId, MANAGE_SECRET_MANAGERS,
        oldScopedEntity.getUsageRestrictions(), newScopedEntity.getUsageRestrictions(),
        newScopedEntity.isScopedToAccount());
  }

  @Override
  public void canSetPermissions(String accountId, ScopedEntity scopedEntity) {
    usageRestrictionsService.validateUsageRestrictionsOnEntitySave(
        accountId, MANAGE_SECRET_MANAGERS, scopedEntity.getUsageRestrictions(), scopedEntity.isScopedToAccount());
  }

  @Override
  public boolean areUsageScopesSubset(String accountId, ScopedEntity scopedEntity, ScopedEntity parentScopedEntity) {
    return usageRestrictionsService.isUsageRestrictionsSubset(
        accountId, scopedEntity.getUsageRestrictions(), parentScopedEntity.getUsageRestrictions());
  }

  @Override
  public UsageRestrictions getMaximalAllowedScopes(String accountId, ScopedEntity scopedEntity) {
    return usageRestrictionsService.getMaximumAllowedUsageRestrictionsForUser(
        accountId, scopedEntity.getUsageRestrictions());
  }

  private boolean hasAccessToReadSMInternal(String accountId, ScopedEntity scopedEntity, String appId, String envId) {
    boolean isAccountAdmin = userService.hasPermission(accountId, MANAGE_SECRET_MANAGERS);
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
}
