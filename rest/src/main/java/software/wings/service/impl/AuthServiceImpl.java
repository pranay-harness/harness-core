package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.ErrorCode.ACCESS_DENIED;
import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.beans.ErrorCode.EXPIRED_TOKEN;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.ErrorCode.INVALID_TOKEN;
import static software.wings.beans.ErrorCode.USER_DOES_NOT_EXIST;
import static software.wings.exception.WingsException.ALERTING;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jwt.EncryptedJWT;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.AuthToken;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.FeatureName;
import software.wings.beans.Permission;
import software.wings.beans.Role;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.dl.GenericDbCache;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.PageRequestBuilder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.AppPermissionSummary;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserRequestInfo;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.CacheHelper;

import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.cache.Cache;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by peeyushaggarwal on 8/18/16.
 */
@Singleton
public class AuthServiceImpl implements AuthService {
  private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

  private GenericDbCache dbCache;
  private WingsPersistence wingsPersistence;
  private UserService userService;
  private UserGroupService userGroupService;
  private WorkflowService workflowService;
  private EnvironmentService environmentService;
  private CacheHelper cacheHelper;
  private MainConfiguration configuration;
  private LearningEngineService learningEngineService;
  private FeatureFlagService featureFlagService;
  private AuthHandler authHandler;

  @Inject
  public AuthServiceImpl(GenericDbCache dbCache, WingsPersistence wingsPersistence, UserService userService,
      UserGroupService userGroupService, WorkflowService workflowService, EnvironmentService environmentService,
      CacheHelper cacheHelper, MainConfiguration configuration, LearningEngineService learningEngineService,
      AuthHandler authHandler, FeatureFlagService featureFlagService) {
    this.dbCache = dbCache;
    this.wingsPersistence = wingsPersistence;
    this.userService = userService;
    this.userGroupService = userGroupService;
    this.workflowService = workflowService;
    this.environmentService = environmentService;
    this.cacheHelper = cacheHelper;
    this.configuration = configuration;
    this.learningEngineService = learningEngineService;
    this.authHandler = authHandler;
    this.featureFlagService = featureFlagService;
  }

  @Override
  public AuthToken validateToken(String tokenString) {
    AuthToken authToken = dbCache.get(AuthToken.class, tokenString);

    if (authToken == null) {
      throw new WingsException(INVALID_TOKEN, ALERTING);
    } else if (authToken.getExpireAt() <= System.currentTimeMillis()) {
      throw new WingsException(EXPIRED_TOKEN, ALERTING);
    }
    User user = getUserFromCacheOrDB(authToken);
    if (user == null) {
      throw new WingsException(USER_DOES_NOT_EXIST);
    }
    authToken.setUser(user);

    return authToken;
  }

  private User getUserFromCacheOrDB(AuthToken authToken) {
    Cache<String, User> userCache = cacheHelper.getUserCache();
    if (userCache == null) {
      logger.warn("userCache is null. Fetch from DB");
      return userService.get(authToken.getUserId());
    } else {
      User user;
      try {
        user = userCache.get(authToken.getUserId());
        if (user == null) {
          user = userService.get(authToken.getUserId());
          userCache.put(user.getUuid(), user);
        }
      } catch (Exception ex) {
        // If there was any exception, remove that entry from cache
        userCache.remove(authToken.getUserId());
        user = userService.get(authToken.getUserId());
        userCache.put(user.getUuid(), user);
      }
      return user;
    }
  }

  private void authorize(String accountId, String appId, String envId, User user,
      List<PermissionAttribute> permissionAttributes, UserRequestInfo userRequestInfo, boolean accountNullCheck) {
    if (!accountNullCheck) {
      if (accountId == null || dbCache.get(Account.class, accountId) == null) {
        logger.error("Auth Failure: non-existing accountId: {}", accountId);
        throw new WingsException(ACCESS_DENIED);
      }
    }

    if (appId != null && dbCache.get(Application.class, appId) == null) {
      logger.error("Auth Failure: non-existing appId: {}", appId);
      throw new WingsException(ACCESS_DENIED);
    }

    if (user.isAccountAdmin(accountId)) {
      return;
    }

    EnvironmentType envType = null;
    if (envId != null) {
      Environment env = dbCache.get(Environment.class, envId);
      envType = env.getEnvironmentType();
    }

    for (PermissionAttribute permissionAttribute : permissionAttributes) {
      if (!authorizeAccessType(accountId, appId, envId, envType, permissionAttribute,
              user.getRolesByAccountId(accountId), userRequestInfo)) {
        throw new WingsException(ACCESS_DENIED);
      }
    }
  }

  @Override
  public void authorize(String accountId, String appId, String envId, User user,
      List<PermissionAttribute> permissionAttributes, UserRequestInfo userRequestInfo) {
    authorize(accountId, appId, envId, user, permissionAttributes, userRequestInfo, true);
  }

  @Override
  public void authorize(String accountId, List<String> appIds, String envId, User user,
      List<PermissionAttribute> permissionAttributes, UserRequestInfo userRequestInfo) {
    if (accountId == null || dbCache.get(Account.class, accountId) == null) {
      logger.error("Auth Failure: non-existing accountId: {}", accountId);
      throw new WingsException(ACCESS_DENIED);
    }

    if (appIds != null) {
      for (String appId : appIds) {
        authorize(accountId, appId, envId, user, permissionAttributes, userRequestInfo, false);
      }
    }
  }

  private void authorize(String accountId, String appId, String entityId, User user,
      List<PermissionAttribute> permissionAttributes, boolean accountNullCheck) {
    if (!accountNullCheck) {
      if (accountId == null || dbCache.get(Account.class, accountId) == null) {
        logger.error("Auth Failure: non-existing accountId: {}", accountId);
        throw new WingsException(ACCESS_DENIED);
      }
    }

    if (appId != null && dbCache.get(Application.class, appId) == null) {
      logger.error("Auth Failure: non-existing appId: {}", appId);
      throw new WingsException(ACCESS_DENIED);
    }

    if (user == null) {
      logger.error("No user context for authorization request for app: {}", appId);
      throw new WingsException(ACCESS_DENIED);
    }

    UserRequestContext userRequestContext = user.getUserRequestContext();
    if (userRequestContext == null) {
      logger.error("User Request Context null for User {}", user.getName());
      throw new WingsException(ACCESS_DENIED);
    }

    UserPermissionInfo userPermissionInfo = userRequestContext.getUserPermissionInfo();
    if (userPermissionInfo == null) {
      logger.error("User permission info null for User {}", user.getName());
      throw new WingsException(ACCESS_DENIED);
    }

    for (PermissionAttribute permissionAttribute : permissionAttributes) {
      if (!authorizeAccessType(appId, entityId, permissionAttribute, userPermissionInfo)) {
        logger.error("User {} not authorized to access requested resource: {}", user.getName(), entityId);
        throw new WingsException(ACCESS_DENIED);
      }
    }
  }

  private List<UserGroup> getUserGroupsByAccountId(String accountId, User user) {
    PageRequest<UserGroup> pageRequest = PageRequestBuilder.aPageRequest()
                                             .addFilter("accountId", Operator.EQ, accountId)
                                             .addFilter("memberIds", Operator.HAS, user.getUuid())
                                             .build();
    PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest);
    return pageResponse.getResponse();
  }

  @Override
  public void authorize(
      String accountId, String appId, String entityId, User user, List<PermissionAttribute> permissionAttributes) {
    authorize(accountId, appId, entityId, user, permissionAttributes, true);
  }

  @Override
  public void authorize(String accountId, List<String> appIds, String entityId, User user,
      List<PermissionAttribute> permissionAttributes) {
    if (accountId == null || dbCache.get(Account.class, accountId) == null) {
      logger.error("Auth Failure: non-existing accountId: {}", accountId);
      throw new WingsException(ACCESS_DENIED);
    }

    if (appIds != null) {
      for (String appId : appIds) {
        authorize(accountId, appId, entityId, user, permissionAttributes, false);
      }
    }
  }

  @Override
  public void validateDelegateToken(String accountId, String tokenString) {
    logger.info("Delegate token validation, account id [{}] token [{}]", accountId, tokenString); // TODO: remove this
    Account account = dbCache.get(Account.class, accountId);
    if (account == null) {
      logger.error("Account Id {} does not exist in manager. So, rejecting delegate register request.", accountId);
      throw new WingsException(ACCESS_DENIED);
    }

    EncryptedJWT encryptedJWT = null;
    try {
      encryptedJWT = EncryptedJWT.parse(tokenString);
    } catch (ParseException e) {
      logger.error("Invalid token for delegate " + tokenString, e);
      throw new WingsException(INVALID_TOKEN);
    }

    byte[] encodedKey = new byte[0];
    try {
      encodedKey = Hex.decodeHex(account.getAccountKey().toCharArray());
    } catch (DecoderException e) {
      logger.error("Invalid hex account key " + account.getAccountKey(), e);
      throw new WingsException(DEFAULT_ERROR_CODE); // ShouldNotHappen
    }

    JWEDecrypter decrypter = null;
    try {
      decrypter = new DirectDecrypter(new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES"));
    } catch (KeyLengthException e) {
      logger.error("Invalid account key " + account.getAccountKey(), e);
      throw new WingsException(DEFAULT_ERROR_CODE);
    }

    try {
      encryptedJWT.decrypt(decrypter);
    } catch (JOSEException e) {
      throw new WingsException(INVALID_TOKEN);
    }
  }

  @Override
  public void validateExternalServiceToken(String accountId, String externalServiceToken) {
    String jwtExternalServiceSecret = configuration.getPortal().getJwtExternalServiceSecret();
    if (isBlank(jwtExternalServiceSecret)) {
      throw new WingsException(INVALID_REQUEST).addParam("message", "incorrect portal setup");
    }
    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtExternalServiceSecret);
      JWTVerifier verifier = JWT.require(algorithm).withIssuer("Harness Inc").build();
      verifier.verify(externalServiceToken);
      JWT decode = JWT.decode(externalServiceToken);
      if (decode.getExpiresAt().getTime() < System.currentTimeMillis()) {
        throw new WingsException(EXPIRED_TOKEN, ALERTING);
      }
    } catch (Exception ex) {
      logger.warn("Error in verifying JWT token ", ex);
      throw ex instanceof JWTVerificationException ? new WingsException(INVALID_TOKEN) : new WingsException(ex);
    }
  }

  @Override
  public void validateLearningEngineServiceToken(String learningEngineServiceToken) {
    String jwtLearningEngineServiceSecret = learningEngineService.getServiceSecretKey(ServiceType.LEARNING_ENGINE);
    if (StringUtils.isBlank(jwtLearningEngineServiceSecret)) {
      throw new WingsException(INVALID_REQUEST)
          .addParam("message", "no secret key for service found for " + ServiceType.LEARNING_ENGINE);
    }
    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtLearningEngineServiceSecret);
      JWTVerifier verifier = JWT.require(algorithm).withIssuer("Harness Inc").build();
      verifier.verify(learningEngineServiceToken);
      JWT decode = JWT.decode(learningEngineServiceToken);
      if (decode.getExpiresAt().getTime() < System.currentTimeMillis()) {
        throw new WingsException(EXPIRED_TOKEN, ALERTING);
      }
    } catch (Exception ex) {
      logger.warn("Error in verifying JWT token ", ex);
      throw ex instanceof JWTVerificationException ? new WingsException(INVALID_TOKEN) : new WingsException(ex);
    }
  }

  @Override
  public void invalidateAllTokensForUser(String userId) {
    List<Key<AuthToken>> keyList =
        wingsPersistence.createQuery(AuthToken.class).field("userId").equal(userId).asKeyList();
    keyList.forEach(authTokenKey -> {
      wingsPersistence.delete(AuthToken.class, authTokenKey.getId().toString());
      dbCache.invalidate(AuthToken.class, authTokenKey.getId().toString());
    });
  }

  private boolean authorizeAccessType(String accountId, String appId, String envId, EnvironmentType envType,
      PermissionAttribute permissionAttribute, List<Role> roles, UserRequestInfo userRequestInfo) {
    return roles.stream()
        .filter(role
            -> roleAuthorizedWithAccessType(
                role, permissionAttribute, accountId, appId, envId, envType, userRequestInfo))
        .findFirst()
        .isPresent();
  }

  private boolean roleAuthorizedWithAccessType(Role role, PermissionAttribute permissionAttribute, String accountId,
      String appId, String envId, EnvironmentType envType, UserRequestInfo userRequestInfo) {
    if (role.getPermissions() == null) {
      return false;
    }

    Action reqAction = permissionAttribute.getAction();
    PermissionType permissionType = permissionAttribute.getPermissionType();

    for (Permission permission : role.getPermissions()) {
      if (permission.getPermissionScope() != permissionType
          || (permission.getAction() != Action.ALL && reqAction != permission.getAction())) {
        continue;
      }
      if (permissionType == PermissionType.APP) {
        if (userRequestInfo != null
            && (userRequestInfo.isAllAppsAllowed() || userRequestInfo.getAllowedAppIds().contains(appId))) {
          return true;
        }
        if (permission.getAppId() != null
            && (permission.getAppId().equals(GLOBAL_APP_ID) || permission.getAppId().equals(appId))) {
          return true;
        }
      } else if (permissionType == PermissionType.ENV) {
        if (userRequestInfo != null
            && (userRequestInfo.isAllEnvironmentsAllowed() || userRequestInfo.getAllowedEnvIds().contains(envId))) {
          return true;
        }

        if (permission.getEnvironmentType() != null && permission.getEnvironmentType().equals(envType)) {
          return true;
        }

        if (permission.getEnvId() != null
            && (permission.getEnvId().equals(GLOBAL_ENV_ID) || permission.getEnvId().equals(envId))) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public UserPermissionInfo getUserPermissionInfo(String accountId, User user) {
    if (!featureFlagService.isEnabled(FeatureName.RBAC, accountId)) {
      return getUserPermissionInfoFromDB(accountId, user);
    }

    Cache<String, UserPermissionInfo> cache = cacheHelper.getUserPermissionInfoCache();
    if (cache == null) {
      logger.error("UserInfoCache is null. This should not happen. Fall back to DB");
      return getUserPermissionInfoFromDB(accountId, user);
    }

    String key = getUserPermissionInfoCacheKey(accountId, user.getUuid());
    UserPermissionInfo value = null;
    try {
      value = cache.get(key);
      if (value == null) {
        value = getUserPermissionInfoFromDB(accountId, user);
        cache.put(key, value);
      }
      return value;
    } catch (Exception ignored) {
      logger.error("Error in fetching user UserPermissionInfo from Cache for key:" + key, ignored);
    }
    // not found in cache. cache write through failed as well. rebuild anyway
    return getUserPermissionInfoFromDB(accountId, user);
  }

  private String getUserPermissionInfoCacheKey(String accountId, String userId) {
    return accountId + "~" + userId;
  }

  @Override
  public void evictAccountUserPermissionInfoCache(String accountId) {
    boolean rbacEnabled = featureFlagService.isEnabled(FeatureName.RBAC, accountId);
    if (!rbacEnabled) {
      return;
    }

    Cache<String, UserPermissionInfo> cache = cacheHelper.getUserPermissionInfoCache();
    Set<String> keys = new HashSet<>();
    if (cache != null) {
      cache.iterator().forEachRemaining(stringUserPermissionInfoEntry -> {
        String key = stringUserPermissionInfoEntry.getKey();
        if (isNotEmpty(key) && key.startsWith(accountId)) {
          keys.add(key);
        }
      });
      cache.removeAll(keys);
    }
  }

  @Override
  public void evictAccountUserPermissionInfoCache(String accountId, List<String> memberIds) {
    boolean rbacEnabled = featureFlagService.isEnabled(FeatureName.RBAC, accountId);
    if (!rbacEnabled) {
      return;
    }

    Cache<String, UserPermissionInfo> cache = cacheHelper.getUserPermissionInfoCache();
    if (cache != null && isNotEmpty(memberIds)) {
      Set<String> keys =
          memberIds.stream().map(userId -> getUserPermissionInfoCacheKey(accountId, userId)).collect(toSet());
      cache.removeAll(keys);
    }
  }

  private UserPermissionInfo getUserPermissionInfoFromDB(String accountId, User user) {
    List<UserGroup> userGroups = getUserGroupsByAccountId(accountId, user);
    return authHandler.getUserPermissionInfo(accountId, userGroups);
  }

  private boolean authorizeAccessType(String appId, String entityId, PermissionAttribute requiredPermissionAttribute,
      UserPermissionInfo userPermissionInfo) {
    if (requiredPermissionAttribute.isSkipAuth()) {
      return true;
    }

    Action requiredAction = requiredPermissionAttribute.getAction();
    PermissionType requiredPermissionType = requiredPermissionAttribute.getPermissionType();

    Map<String, AppPermissionSummary> appPermissionMap = userPermissionInfo.getAppPermissionMapInternal();
    AppPermissionSummary appPermissionSummary = appPermissionMap.get(appId);

    if (appPermissionSummary == null) {
      return false;
    }

    if (Action.CREATE == requiredAction) {
      if (requiredPermissionType == PermissionType.SERVICE) {
        return appPermissionSummary.isCanCreateService();
      } else if (requiredPermissionType == PermissionType.ENV) {
        return appPermissionSummary.isCanCreateEnvironment();
      } else if (requiredPermissionType == PermissionType.WORKFLOW) {
        return appPermissionSummary.isCanCreateWorkflow();
      } else if (requiredPermissionType == PermissionType.PIPELINE) {
        return appPermissionSummary.isCanCreatePipeline();
      } else {
        String msg = "Unsupported app permission entity type: " + requiredPermissionType;
        logger.error(msg);
        throw new WingsException(msg);
      }
    }

    Map<Action, Set<String>> actionEntityIdMap;

    if (requiredPermissionType == PermissionType.SERVICE) {
      actionEntityIdMap = appPermissionSummary.getServicePermissions();
    } else if (requiredPermissionType == PermissionType.ENV) {
      actionEntityIdMap = appPermissionSummary.getEnvPermissions();
    } else if (requiredPermissionType == PermissionType.WORKFLOW) {
      actionEntityIdMap = appPermissionSummary.getWorkflowPermissions();
    } else if (requiredPermissionType == PermissionType.PIPELINE) {
      actionEntityIdMap = appPermissionSummary.getPipelinePermissions();
    } else if (requiredPermissionType == PermissionType.DEPLOYMENT) {
      actionEntityIdMap = appPermissionSummary.getDeploymentPermissions();
    } else {
      String msg = "Unsupported app permission entity type: " + requiredPermissionType;
      logger.error(msg);
      throw new WingsException(msg);
    }

    if (actionEntityIdMap == null) {
      return false;
    }

    Collection<String> entityIds = actionEntityIdMap.get(requiredAction);
    if (CollectionUtils.isEmpty(entityIds)) {
      return false;
    }

    return entityIds.contains(entityId);
  }
}
