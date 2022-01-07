/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;

import software.wings.beans.User;
import software.wings.search.entities.related.deployment.RelatedDeploymentView;
import software.wings.search.framework.EntityInfo;
import software.wings.security.AppPermissionSummary;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public final class SearchPermissionUtils {
  public static UserPermissionInfo getUserPermission() {
    User user = UserThreadLocal.get();
    UserRequestContext userRequestContext = user.getUserRequestContext();
    return userRequestContext.getUserPermissionInfo();
  }

  public static Set<String> getAllowedWorkflowIds(AppPermissionSummary appPermission) {
    if (appPermission.getWorkflowPermissions() != null
        && appPermission.getWorkflowPermissions().get(PermissionAttribute.Action.READ) != null) {
      return appPermission.getWorkflowPermissions().get(PermissionAttribute.Action.READ);
    }
    return new HashSet<>();
  }

  public static Set<String> getAllowedPiplineIds(AppPermissionSummary appPermission) {
    if (appPermission.getPipelinePermissions() != null
        && appPermission.getPipelinePermissions().get(PermissionAttribute.Action.READ) != null) {
      return appPermission.getPipelinePermissions().get(PermissionAttribute.Action.READ);
    }
    return new HashSet<>();
  }

  public static Set<String> getAllowedServiceIds(AppPermissionSummary appPermission) {
    if (appPermission.getServicePermissions() != null
        && appPermission.getServicePermissions().get(PermissionAttribute.Action.READ) != null) {
      return appPermission.getServicePermissions().get(PermissionAttribute.Action.READ);
    }
    return new HashSet<>();
  }

  public static Set<String> getAllowedEnvironmentIds(AppPermissionSummary appPermission) {
    Set<String> environmentPermissions = new HashSet<>(Collections.emptySet());
    if (appPermission.getEnvPermissions() != null) {
      Set<AppPermissionSummary.EnvInfo> temp = appPermission.getEnvPermissions().get(PermissionAttribute.Action.READ);
      if (temp != null) {
        for (AppPermissionSummary.EnvInfo env : temp) {
          environmentPermissions.add(env.getEnvId());
        }
      }
    }
    return environmentPermissions;
  }

  public static Set<String> getAllowedDeploymentIds(AppPermissionSummary appPermission) {
    if (appPermission.getDeploymentPermissions() != null
        && appPermission.getDeploymentPermissions().get(PermissionAttribute.Action.READ) != null) {
      return appPermission.getDeploymentPermissions().get(PermissionAttribute.Action.READ);
    }
    return new HashSet<>();
  }

  public static Set<EntityInfo> getAllowedEntities(Set<EntityInfo> entities, Set<String> allowedEntityIds) {
    if (EmptyPredicate.isEmpty(entities) || EmptyPredicate.isEmpty(allowedEntityIds)) {
      return new HashSet<>();
    }
    return entities.stream().filter(entity -> allowedEntityIds.contains(entity.getId())).collect(Collectors.toSet());
  }

  public static boolean hasAuditPermissions(UserPermissionInfo userPermissionInfo) {
    return userPermissionInfo.getAccountPermissionSummary().getPermissions().contains(
        PermissionAttribute.PermissionType.AUDIT_VIEWER);
  }

  public static boolean hasDeploymentPermissions(Set<String> allowedIds, String id) {
    return allowedIds.contains(id);
  }

  public static List<RelatedDeploymentView> getAllowedDeployments(
      List<RelatedDeploymentView> deployments, Set<String> allowedDeploymentIds) {
    if (EmptyPredicate.isEmpty(deployments) || EmptyPredicate.isEmpty(allowedDeploymentIds)) {
      return new ArrayList<>();
    }
    return deployments.stream()
        .filter(deployment -> allowedDeploymentIds.contains(deployment.getWorkflowId()))
        .collect(Collectors.toList());
  }
}
