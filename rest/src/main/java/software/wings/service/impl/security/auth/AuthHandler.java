package software.wings.service.impl.security.auth;

import software.wings.beans.Account;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author rktummala on 3/7/18
 */
public interface AuthHandler {
  UserPermissionInfo getUserPermissionInfo(String accountId, List<UserGroup> userGroups);

  <T extends Base> List<T> getAllEntities(PageRequest<T> pageRequest, Callable<PageResponse<T>> callable);

  Set<String> getAppIdsByFilter(Set<String> allAppIds, GenericEntityFilter appFilter);
  Set<String> getAppIdsByFilter(String accountId, GenericEntityFilter appFilter);
  Set<String> getEnvIdsByFilter(Set<Environment> appIds, EnvFilter envFilter);
  Set<String> getEnvIdsByFilter(String appId, EnvFilter envFilter);
  void setEntityIdFilterIfUserAction(List<PermissionAttribute> requiredPermissionAttributes, List<String> appIds);
  void setAppIdFilter(UserRequestContext userRequestContext, Set<String> appIds);

  boolean authorize(List<PermissionAttribute> requiredPermissionAttributes, List<String> appIds, String entityId);

  void setEntityIdFilter(List<PermissionAttribute> requiredPermissionAttributes, UserRequestContext userRequestContext,
      List<String> appIds);
  void setEntityIdFilterIfGet(String httpMethod, boolean skipAuth,
      List<PermissionAttribute> requiredPermissionAttributes, UserRequestContext userRequestContext,
      boolean appIdFilterRequired, Set<String> allowedAppIds, List<String> appIdsFromRequest);

  UserGroup buildDefaultAdminUserGroup(String accountId, User user);
  UserGroup buildReadOnlyUserGroup(String accountId, User user, String userGroupName);

  UserGroup buildProdSupportUserGroup(String accountId);
  UserGroup buildNonProdSupportUserGroup(String accountId);

  void addUserToDefaultAccountAdminUserGroup(User user, Account account);
  void createDefaultUserGroups(Account account, User user);
  <T extends Base> void setFilter(String accountId, String appId, PageRequest<T> pageRequest);

  <T extends Base> void setFilter(String accountId, List<String> appIdList, PageRequest<T> pageRequest);
}
