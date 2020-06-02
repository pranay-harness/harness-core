package software.wings.service.intfc;

import software.wings.beans.AuthToken;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Pipeline;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.security.UserGroup;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestInfo;
import software.wings.security.UserRestrictionInfo;

import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

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

  void invalidateToken(String utoken);

  void validateExternalServiceToken(String accountId, String tokenString);

  void validateLearningEngineServiceToken(String learningEngineServiceToken);

  UserPermissionInfo getUserPermissionInfo(String accountId, User user, boolean cacheOnly);

  UserRestrictionInfo getUserRestrictionInfo(
      String accountId, User user, UserPermissionInfo userPermissionInfo, boolean cacheOnly);

  void evictPermissionAndRestrictionCacheForUserGroup(UserGroup userGroup);

  void evictUserPermissionAndRestrictionCacheForAccount(String accountId, List<String> memberIds);

  void evictUserPermissionAndRestrictionCacheForAccount(
      String accountId, boolean rebuildUserPermissionInfo, boolean rebuildUserRestrictionInfo);

  void evictUserPermissionCacheForAccount(String accountId, boolean rebuildUserPermissionInfo);

  void evictUserPermissionAndRestrictionCacheForAccounts(Set<String> accountIds, List<String> memberIds);

  UserRestrictionInfo getUserRestrictionInfoFromDB(
      String accountId, UserPermissionInfo userPermissionInfo, List<UserGroup> userGroupList);

  User refreshToken(String oldToken);

  User generateBearerTokenForUser(@NotNull User user);

  void checkIfUserAllowedToDeployToEnv(String appId, String envId);

  void checkIfUserCanCreateEnv(String appId, EnvironmentType envType);

  void checkWorkflowPermissionsForEnv(String appId, Workflow workflow, Action action);

  void checkIfUserCanCloneWorkflowToOtherApp(String targetAppId, Workflow workflow);

  void checkPipelinePermissionsForEnv(String appId, Pipeline pipeline, Action action);

  void authorizeAppAccess(String accountId, String appId, User user, Action action);

  void auditLogin(User loggedInUser);
}
