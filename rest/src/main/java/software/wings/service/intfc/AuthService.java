package software.wings.service.intfc;

import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.security.AppPermissionSummary;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestInfo;

import java.util.List;
import java.util.Set;

/**
 * Created by peeyushaggarwal on 8/18/16.
 */
public interface AuthService {
  /**
   * Validate token auth token.
   *
   * @param tokenString the token string
   * @return the auth token
   */
  AuthToken validateToken(String tokenString);

  /**
   * Authorize.
   * @param accountId
   * @param appId                the app id
   * @param envId                the env id
   * @param user                 the user
   * @param permissionAttributes the permission attributes
   * @param userRequestInfo
   */
  void authorize(String accountId, String appId, String envId, User user,
      List<PermissionAttribute> permissionAttributes, UserRequestInfo userRequestInfo);

  /**
   * Authorize.
   * @param accountId
   * @param appIds               list of app ids
   * @param envId                the env id
   * @param user                 the user
   * @param permissionAttributes the permission attributes
   * @param userRequestInfo
   */
  void authorize(String accountId, List<String> appIds, String envId, User user,
      List<PermissionAttribute> permissionAttributes, UserRequestInfo userRequestInfo);

  /**
   * Authorize.
   * @param accountId
   * @param appId                the app id
   * @param entityId             the entity id
   * @param user                 the user
   * @param permissionAttributes the permission attributes
   */
  void authorize(
      String accountId, String appId, String entityId, User user, List<PermissionAttribute> permissionAttributes);

  /**
   * Authorize.
   * @param accountId
   * @param appIds               list of app ids
   * @param entityId             the entity id
   * @param user                 the user
   * @param permissionAttributes the permission attributes
   */
  void authorize(String accountId, List<String> appIds, String entityId, User user,
      List<PermissionAttribute> permissionAttributes);

  void validateDelegateToken(String accountId, String tokenString);

  void invalidateAllTokensForUser(String userId);

  void validateExternalServiceToken(String accountId, String tokenString);

  void validateLearningEngineServiceToken(String learningEngineServiceToken);

  UserPermissionInfo getUserPermissionInfo(String accountId, User user);

  Set<String> getAppPermissionsForUser(
      String userId, String accountId, String appId, PermissionType permissionType, Action action);

  AppPermissionSummary getAppPermissionSummaryForUser(String userId, String accountId, String appId);

  void evictAccountUserPermissionInfoCache(String accountId, boolean rebuild);

  void evictAccountUserPermissionInfoCache(String accountId, List<String> memberIds);

  void evictAccountUserPermissionInfoCache(Set<String> accountIds, List<String> memberIds);
}
