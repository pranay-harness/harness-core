package software.wings.service.impl.security.auth;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.dashboard.Action;
import io.harness.dashboard.DashboardAccessPermissions;
import io.harness.dashboard.DashboardSettings;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import org.mongodb.morphia.query.Query;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.security.AccountPermissionSummary;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.UserGroupService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author rktummala on 07/01/19
 */
@Singleton
public class DashboardAuthHandler {
  @Inject private UserGroupService userGroupService;
  @Inject private WingsPersistence wingsPersistence;
  private Set<Action> allActions = Sets.newHashSet(Action.READ, Action.UPDATE, Action.DELETE);

  public Map<String, Set<Action>> getDashboardAccessPermissions(
      User user, String accountId, UserPermissionInfo userPermissionInfo) {
    boolean accountAdmin = isAccountAdmin(userPermissionInfo);

    Map<String, Set<Action>> dashboardActionMap = new HashMap<>();
    Query<DashboardSettings> query = wingsPersistence.createQuery(DashboardSettings.class);
    query.filter("accountId", accountId);
    if (!accountAdmin) {
      List<String> userGroupIdList = userGroupService.getUserGroupIdsByAccountId(accountId, user);
      if (isNotEmpty(userGroupIdList)) {
        query.or(query.criteria("createdBy.uuid").equal(user.getUuid()),
            query.criteria("permissions.userGroups").in(userGroupIdList));
      } else {
        query.filter("createdBy", user.getUuid());
      }
    }

    try (HIterator<DashboardSettings> iterator = new HIterator<>(query.fetch())) {
      while (iterator.hasNext()) {
        DashboardSettings dashboardSettings = iterator.next();
        if (dashboardSettings == null) {
          continue;
        }

        boolean owner = dashboardSettings.getCreatedBy().getUuid().equals(user.getUuid());
        if (accountAdmin || owner) {
          Set<Action> actions = dashboardActionMap.putIfAbsent(dashboardSettings.getUuid(), new HashSet<>());
          if (actions == null) {
            actions = dashboardActionMap.get(dashboardSettings.getUuid());
          }
          actions.addAll(allActions);
          continue;
        }

        List<DashboardAccessPermissions> permissions = dashboardSettings.getPermissions();

        if (permissions == null) {
          continue;
        }
        permissions.forEach(permission -> {
          if (permission == null) {
            return;
          }
          List<String> userGroupIds = permission.getUserGroups();
          if (userGroupIds == null) {
            return;
          }

          Set<Action> actions = dashboardActionMap.putIfAbsent(dashboardSettings.getUuid(), new HashSet<>());
          if (actions == null) {
            actions = dashboardActionMap.get(dashboardSettings.getUuid());
          }
          actions.addAll(permission.getAllowedActions());
        });
      }
    }

    return dashboardActionMap;
  }

  public void setAccessFlags(List<DashboardSettings> dashboardSettingsList) {
    User user = UserThreadLocal.get();

    if (user == null) {
      return;
    }

    String userId = user.getUuid();

    UserPermissionInfo userPermissionInfo = user.getUserRequestContext().getUserPermissionInfo();
    Map<String, Set<Action>> dashboardPermissions = userPermissionInfo.getDashboardPermissions();

    dashboardSettingsList.forEach(dashboardSettings -> {
      if (dashboardSettings.getCreatedBy().getUuid().equals(userId)) {
        dashboardSettings.setOwner(true);
      }

      if (isAccountAdmin(userPermissionInfo)) {
        dashboardSettings.setCanUpdate(true);
        dashboardSettings.setCanDelete(true);
        return;
      }

      Set<Action> actions = dashboardPermissions.get(dashboardSettings.getUuid());
      if (isNotEmpty(actions)) {
        dashboardSettings.setCanUpdate(actions.contains(Action.UPDATE));
        dashboardSettings.setCanDelete(actions.contains(Action.DELETE));
      }
    });
  }

  private boolean isAccountAdmin(UserPermissionInfo userPermissionInfo) {
    AccountPermissionSummary accountPermissionSummary = userPermissionInfo.getAccountPermissionSummary();
    if (accountPermissionSummary == null) {
      return false;
    }

    Set<PermissionType> permissions = accountPermissionSummary.getPermissions();
    if (isEmpty(permissions)) {
      return false;
    }

    return permissions.contains(PermissionType.ACCOUNT_MANAGEMENT);
  }

  public void authorize(DashboardSettings dashboardSettings, String accountId, Action action) {
    if (dashboardSettings == null) {
      return;
    }

    if (!dashboardSettings.getAccountId().equals(accountId)) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
    }

    User user = UserThreadLocal.get();

    if (user == null) {
      return;
    }

    UserPermissionInfo userPermissionInfo = user.getUserRequestContext().getUserPermissionInfo();
    Map<String, Set<Action>> dashboardPermissions = userPermissionInfo.getDashboardPermissions();

    if (isEmpty(dashboardPermissions)) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
    }

    Set<Action> actions = dashboardPermissions.get(dashboardSettings.getUuid());
    if (isEmpty(actions)) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
    }

    if (!actions.contains(action)) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
    }
  }

  public Set<String> getAllowedDashboardSettingIds() {
    User user = UserThreadLocal.get();

    if (user == null) {
      return null;
    }

    UserPermissionInfo userPermissionInfo = user.getUserRequestContext().getUserPermissionInfo();
    Map<String, Set<Action>> dashboardPermissions = userPermissionInfo.getDashboardPermissions();

    if (isEmpty(dashboardPermissions)) {
      return null;
    }

    return dashboardPermissions.keySet();
  }
}
