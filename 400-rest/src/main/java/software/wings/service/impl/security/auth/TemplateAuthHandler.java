/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.security.auth;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.security.PermissionAttribute.Action;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.User;
import software.wings.beans.template.Template;
import software.wings.beans.template.Template.TemplateKeys;
import software.wings.dl.WingsPersistence;
import software.wings.security.AppPermissionSummary;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.security.auth.TemplateRBACListFilter.TemplateRBACListFilterBuilder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(PL)
@TargetModule(HarnessModule.UNDEFINED)
public class TemplateAuthHandler {
  @Inject private WingsPersistence wingsPersistence;

  public TemplateRBACListFilter buildTemplateListRBACFilter(List<String> appIds) {
    if (isEmpty(appIds)) {
      throw new InvalidRequestException("Invalid Arguments while fetching templates, no application ids are provided.");
    }
    final TemplateRBACListFilterBuilder builder = TemplateRBACListFilter.builder();
    User user = UserThreadLocal.get();
    if (user == null) {
      return builder.appIds(new HashSet<>(appIds)).build();
    }
    final Set<String> templateIdsToQuery = new HashSet<>();
    final Set<String> appIdsToQuery = new HashSet<>();
    final Map<String, AppPermissionSummary> appPermissionMapInternal =
        user.getUserRequestContext().getUserPermissionInfo().getAppPermissionMapInternal();
    appIds.forEach(appId -> {
      if (appId.equals(GLOBAL_APP_ID)) {
        appIdsToQuery.add(GLOBAL_APP_ID);
      } else {
        final AppPermissionSummary appPermissionSummary = appPermissionMapInternal.get(appId);
        if (appPermissionSummary != null) {
          final Map<Action, Set<String>> templatePermissions = appPermissionSummary.getTemplatePermissions();
          if (templatePermissions != null) {
            final Set<String> templateIds = templatePermissions.get(Action.READ);
            if (isNotEmpty(templateIds)) {
              templateIdsToQuery.addAll(templateIds);
              appIdsToQuery.add(appId);
            }
          }
        }
      }
    });
    return builder.appIds(appIdsToQuery).templateIds(templateIdsToQuery).build();
  }

  private String getEnumName(Action action) {
    switch (action) {
      case CREATE:
        return Action.CREATE.name().toLowerCase();
      case DELETE:
        return Action.DELETE.name().toLowerCase();
      case UPDATE:
        return Action.UPDATE.name().toLowerCase();
      default:
        return "";
    }
  }

  private void verifyAccountCrudPermissionsForUser(Action action, User user) {
    final Set<PermissionType> templateAccountPermissions =
        user.getUserRequestContext().getUserPermissionInfo().getAccountPermissionSummary().getPermissions();
    if (isEmpty(templateAccountPermissions)
        || !templateAccountPermissions.contains(PermissionType.TEMPLATE_MANAGEMENT)) {
      throw new InvalidRequestException(
          String.format("User doesn't have rights to %s template at account level", getEnumName(action)),
          ErrorCode.ACCESS_DENIED, USER);
    }
  }

  private void verifyAppCrudPermissionsOnTemplateIdForUser(String appId, String templateId, Action action, User user) {
    final AppPermissionSummary appPermissionSummary =
        user.getUserRequestContext().getUserPermissionInfo().getAppPermissionMapInternal().get(appId);
    if (appPermissionSummary == null) {
      throw new InvalidRequestException(
          String.format("User has no permissions on app %s", appId), ErrorCode.ACCESS_DENIED, USER);
    }
    final Map<Action, Set<String>> templateAppPermissions = appPermissionSummary.getTemplatePermissions();
    if (isEmpty(templateAppPermissions)) {
      throw new InvalidRequestException(
          String.format("User has no template permissions in app %s", appId), ErrorCode.ACCESS_DENIED, USER);
    }
    final Set<String> templateCrudActionPermissions = templateAppPermissions.get(action);
    if (isEmpty(templateCrudActionPermissions) || !templateCrudActionPermissions.contains(templateId)) {
      throw new InvalidRequestException(
          String.format("User doesn't have rights to %s template in app %s", getEnumName(action), appId),
          ErrorCode.ACCESS_DENIED, USER);
    }
  }

  public void authorizeCreate(String appId) {
    if (isEmpty(appId)) {
      throw new InvalidRequestException("Invalid argument, appId cannot be null/empty.");
    }
    User user = UserThreadLocal.get();
    if (user == null) {
      return;
    }
    if (!GLOBAL_APP_ID.equals(appId)) {
      final AppPermissionSummary appPermissionSummary =
          user.getUserRequestContext().getUserPermissionInfo().getAppPermissionMapInternal().get(appId);
      if (appPermissionSummary == null || !appPermissionSummary.isCanCreateTemplate()) {
        throw new InvalidRequestException(String.format("User doesn't have rights to create template in app %s", appId),
            ErrorCode.ACCESS_DENIED, USER);
      }
    } else {
      verifyAccountCrudPermissionsForUser(Action.CREATE, user);
    }
  }

  public void authorizeRead(String appId, String templateId) {
    if (isEmpty(appId)) {
      throw new InvalidRequestException("Invalid argument, appId cannot be null/empty.");
    }
    notNullCheck("Invalid argument, template id cannot be empty", templateId);
    User user = UserThreadLocal.get();
    if (user == null) {
      return;
    }
    if (!GLOBAL_APP_ID.equals(appId)) {
      verifyAppCrudPermissionsOnTemplateIdForUser(appId, templateId, Action.READ, user);
    }
    // All account level template reads need not be authorized because as we don't check TEMPLATE_MANAGEMENT permission
    // for it
  }

  public void authorizeUpdate(String appId, String templateId) {
    if (isEmpty(appId)) {
      throw new InvalidRequestException("Invalid argument, appId cannot be null/empty.");
    }
    notNullCheck("Invalid argument, template id cannot be empty", templateId);
    User user = UserThreadLocal.get();
    if (user == null) {
      return;
    }
    if (!GLOBAL_APP_ID.equals(appId)) {
      verifyAppCrudPermissionsOnTemplateIdForUser(appId, templateId, Action.UPDATE, user);
    } else {
      verifyAccountCrudPermissionsForUser(Action.UPDATE, user);
    }
  }

  public void authorizeDelete(String accountId, String templateId) {
    notNullCheck("Invalid argument, template id cannot be empty", templateId);
    User user = UserThreadLocal.get();
    if (user == null) {
      return;
    }
    Template template = wingsPersistence.createQuery(Template.class)
                            .filter(TemplateKeys.accountId, accountId)
                            .filter(TemplateKeys.uuid, templateId)
                            .get();
    if (template != null) {
      final String templateAppId = template.getAppId();
      if (!GLOBAL_APP_ID.equals(templateAppId)) {
        verifyAppCrudPermissionsOnTemplateIdForUser(templateAppId, templateId, Action.DELETE, user);
      } else {
        verifyAccountCrudPermissionsForUser(Action.DELETE, user);
      }
    } else {
      throw new InvalidRequestException(
          String.format("Template with id %s not found", templateId), ErrorCode.TEMPLATE_NOT_FOUND, USER);
    }
  }

  public void authorizeTemplateFolderCrud(String appId, Action action) {
    if (isEmpty(appId)) {
      throw new InvalidRequestException("Invalid argument, appId cannot be null/empty.");
    }
    User user = UserThreadLocal.get();
    if (user == null) {
      return;
    }
    if (!GLOBAL_APP_ID.equals(appId)) {
      final AppPermissionSummary appPermissionSummary =
          user.getUserRequestContext().getUserPermissionInfo().getAppPermissionMapInternal().get(appId);
      if (appPermissionSummary == null) {
        throw new InvalidRequestException(
            String.format("User has no permissions on app %s", appId), ErrorCode.ACCESS_DENIED, USER);
      }
      final Map<Action, Set<String>> templatePermissions = appPermissionSummary.getTemplatePermissions();
      if (templatePermissions != null) {
        final Set<String> templateIdsWithCrudPermissions = templatePermissions.get(Action.READ);
        if (templateIdsWithCrudPermissions.isEmpty()) {
          throw new InvalidRequestException(
              String.format("User doesn't have rights to %s template folder in app %s", getEnumName(action), appId),
              ErrorCode.ACCESS_DENIED, USER);
        }
      } else {
        throw new InvalidRequestException(
            String.format("User doesn't have rights to %s template folder in app %s", getEnumName(action), appId),
            ErrorCode.ACCESS_DENIED, USER);
      }
    } else {
      verifyAccountCrudPermissionsForUser(action, user);
    }
  }
}
