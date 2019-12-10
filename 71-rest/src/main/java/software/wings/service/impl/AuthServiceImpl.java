package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.ACCESS_DENIED;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.ErrorCode.EXPIRED_TOKEN;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.eraro.ErrorCode.TOKEN_ALREADY_REFRESHED_ONCE;
import static io.harness.eraro.ErrorCode.USER_DOES_NOT_EXIST;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.security.PermissionAttribute.Action.CREATE;
import static software.wings.security.PermissionAttribute.Action.DELETE;
import static software.wings.security.PermissionAttribute.Action.EXECUTE;
import static software.wings.security.PermissionAttribute.Action.READ;
import static software.wings.security.PermissionAttribute.Action.UPDATE;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.APPLICATION_CREATE_DELETE;
import static software.wings.security.PermissionAttribute.PermissionType.AUDIT_VIEWER;
import static software.wings.security.PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_READ;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jwt.EncryptedJWT;
import io.harness.eraro.ErrorCode;
import io.harness.event.handler.impl.segment.SegmentHandler;
import io.harness.event.usagemetrics.UsageMetricsEventPublisher;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.HPersistence;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.Key;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.AuthToken;
import software.wings.beans.AuthToken.AuthTokenKeys;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Permission;
import software.wings.beans.Pipeline;
import software.wings.beans.Role;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.dl.GenericDbCache;
import software.wings.dl.WingsPersistence;
import software.wings.logcontext.UserLogContext;
import software.wings.security.AccountPermissionSummary;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.AppPermissionSummaryForUI;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.SecretManager;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserRequestInfo;
import software.wings.security.UserRestrictionInfo;
import software.wings.security.UserRestrictionInfo.UserRestrictionInfoBuilder;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.impl.security.auth.DashboardAuthHandler;
import software.wings.service.intfc.ApiKeyService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WhitelistService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.CacheManager;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.cache.Cache;
import javax.crypto.spec.SecretKeySpec;

@Singleton
@Slf4j
public class AuthServiceImpl implements AuthService {
  private GenericDbCache dbCache;
  private HPersistence persistence;
  private UserService userService;
  private UserGroupService userGroupService;
  private UsageRestrictionsService usageRestrictionsService;
  private WorkflowService workflowService;
  private EnvironmentService environmentService;
  private CacheManager cacheManager;
  private MainConfiguration configuration;
  private LearningEngineService learningEngineService;
  private FeatureFlagService featureFlagService;
  private AuthHandler authHandler;
  private HarnessUserGroupService harnessUserGroupService;
  private SecretManager secretManager;
  private UsageMetricsEventPublisher usageMetricsEventPublisher;
  private WhitelistService whitelistService;
  private SSOSettingService ssoSettingService;
  @Inject private ExecutorService executorService;
  @Inject private ApiKeyService apiKeyService;
  @Inject @Nullable private SegmentHandler segmentHandler;
  private AppService appService;
  private DashboardAuthHandler dashboardAuthHandler;

  @Inject
  public AuthServiceImpl(GenericDbCache dbCache, WingsPersistence persistence, UserService userService,
      UserGroupService userGroupService, UsageRestrictionsService usageRestrictionsService,
      WorkflowService workflowService, EnvironmentService environmentService, CacheManager cacheManager,
      MainConfiguration configuration, LearningEngineService learningEngineService, AuthHandler authHandler,
      FeatureFlagService featureFlagService, HarnessUserGroupService harnessUserGroupService,
      SecretManager secretManager, UsageMetricsEventPublisher usageMetricsEventPublisher,
      WhitelistService whitelistService, SSOSettingService ssoSettingService, AppService appService,
      DashboardAuthHandler dashboardAuthHandler) {
    this.dbCache = dbCache;
    this.persistence = persistence;
    this.userService = userService;
    this.userGroupService = userGroupService;
    this.usageRestrictionsService = usageRestrictionsService;
    this.workflowService = workflowService;
    this.environmentService = environmentService;
    this.cacheManager = cacheManager;
    this.configuration = configuration;
    this.learningEngineService = learningEngineService;
    this.authHandler = authHandler;
    this.featureFlagService = featureFlagService;
    this.harnessUserGroupService = harnessUserGroupService;
    this.secretManager = secretManager;
    this.usageMetricsEventPublisher = usageMetricsEventPublisher;
    this.whitelistService = whitelistService;
    this.ssoSettingService = ssoSettingService;
    this.appService = appService;
    this.dashboardAuthHandler = dashboardAuthHandler;
  }

  @UtilityClass
  public static final class Keys {
    public static final String HARNESS_EMAIL = "@harness.io";
    public static final String LOGIN_EVENT = "User Authenticated";
  }

  @Override
  public AuthToken validateToken(String tokenString) {
    if (tokenString.length() <= 32) {
      AuthToken authToken = dbCache.get(AuthToken.class, tokenString);

      if (authToken == null) {
        throw new WingsException(INVALID_TOKEN, USER);
      } else if (authToken.getExpireAt() <= System.currentTimeMillis()) {
        throw new WingsException(EXPIRED_TOKEN, USER);
      }
      return getAuthTokenWithUser(authToken);
    } else {
      return getAuthTokenWithUser(verifyToken(tokenString));
    }
  }

  private AuthToken verifyToken(String tokenString) {
    AuthToken authToken = verifyJWTToken(tokenString);
    if (authToken == null) {
      throw new WingsException(INVALID_TOKEN, USER);
    }
    return authToken;
  }

  private AuthToken getAuthTokenWithUser(AuthToken authToken) {
    User user = getUserFromCacheOrDB(authToken.getUserId());
    if (user == null) {
      throw new WingsException(USER_DOES_NOT_EXIST);
    }
    authToken.setUser(user);

    return authToken;
  }

  private User getUserFromCacheOrDB(String userId) {
    Cache<String, User> userCache = cacheManager.getUserCache();
    if (userCache == null) {
      logger.warn("userCache is null. Fetch from DB");
      return userService.get(userId);
    } else {
      User user;
      try {
        user = userCache.get(userId);
        if (user == null) {
          user = userService.get(userId);
          userCache.put(user.getUuid(), user);
        }
      } catch (Exception ex) {
        // If there was any exception, remove that entry from cache
        userCache.remove(userId);
        user = userService.get(userId);
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
    UserPermissionInfo userPermissionInfo = authorizeAndGetUserPermissionInfo(accountId, appId, user, accountNullCheck);

    for (PermissionAttribute permissionAttribute : permissionAttributes) {
      if (!authorizeAccessType(appId, entityId, permissionAttribute, userPermissionInfo)) {
        logger.warn("User {} not authorized to access requested resource: {}", user.getName(), entityId);
        throw new WingsException(ACCESS_DENIED, USER);
      }
    }
  }

  @NotNull
  private UserPermissionInfo authorizeAndGetUserPermissionInfo(
      String accountId, String appId, User user, boolean accountNullCheck) {
    if (!accountNullCheck) {
      if (accountId == null || dbCache.get(Account.class, accountId) == null) {
        logger.error("Auth Failure: non-existing accountId: {}", accountId);
        throw new WingsException(ACCESS_DENIED, USER);
      }
    }

    if (appId != null && dbCache.get(Application.class, appId) == null) {
      logger.error("Auth Failure: non-existing appId: {}", appId);
      throw new WingsException(ACCESS_DENIED, USER);
    }

    if (user == null) {
      logger.error("No user context for authorization request for app: {}", appId);
      throw new WingsException(ACCESS_DENIED, USER);
    }

    UserRequestContext userRequestContext = user.getUserRequestContext();
    if (userRequestContext == null) {
      logger.error("User Request Context null for User {}", user.getName());
      throw new WingsException(ACCESS_DENIED, USER);
    }

    UserPermissionInfo userPermissionInfo = userRequestContext.getUserPermissionInfo();
    if (userPermissionInfo == null) {
      logger.error("User permission info null for User {}", user.getName());
      throw new WingsException(ACCESS_DENIED, USER);
    }
    return userPermissionInfo;
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
      throw new WingsException(ACCESS_DENIED, USER);
    }

    if (appIds != null) {
      for (String appId : appIds) {
        authorize(accountId, appId, entityId, user, permissionAttributes, false);
      }
    }
  }

  @Override
  public void validateDelegateToken(String accountId, String tokenString) {
    logger.info("Delegate token validation, account id [{}] token requested", accountId);
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

    byte[] encodedKey;
    try {
      encodedKey = Hex.decodeHex(account.getAccountKey().toCharArray());
    } catch (DecoderException e) {
      logger.error("Invalid hex account key " + account.getAccountKey(), e);
      throw new WingsException(DEFAULT_ERROR_CODE); // ShouldNotHappen
    }

    JWEDecrypter decrypter;
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
      throw new InvalidRequestException("incorrect portal setup");
    }
    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtExternalServiceSecret);
      JWTVerifier verifier = JWT.require(algorithm).withIssuer("Harness Inc").build();
      verifier.verify(externalServiceToken);
      JWT decode = JWT.decode(externalServiceToken);
      if (decode.getExpiresAt().getTime() < System.currentTimeMillis()) {
        throw new WingsException(EXPIRED_TOKEN, USER_ADMIN);
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
      throw new InvalidRequestException("no secret key for service found for " + ServiceType.LEARNING_ENGINE);
    }
    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtLearningEngineServiceSecret);
      JWTVerifier verifier =
          JWT.require(algorithm).withIssuer("Harness Inc").acceptIssuedAt(TimeUnit.MINUTES.toSeconds(10)).build();
      verifier.verify(learningEngineServiceToken);
      JWT decode = JWT.decode(learningEngineServiceToken);
      if (decode.getExpiresAt().getTime() < System.currentTimeMillis()) {
        throw new WingsException(EXPIRED_TOKEN, USER_ADMIN);
      }
    } catch (Exception ex) {
      logger.warn("Error in verifying JWT token ", ex);
      throw ex instanceof JWTVerificationException ? new WingsException(INVALID_TOKEN) : new WingsException(ex);
    }
  }

  @Override
  public void invalidateAllTokensForUser(String userId) {
    List<Key<AuthToken>> keyList =
        persistence.createQuery(AuthToken.class, excludeAuthority).filter(AuthTokenKeys.userId, userId).asKeyList();
    keyList.forEach(authToken -> invalidateToken(authToken.getId().toString()));
  }

  @Override
  public void invalidateToken(String utoken) {
    persistence.delete(AuthToken.class, utoken);
    dbCache.invalidate(AuthToken.class, utoken);
  }

  private boolean authorizeAccessType(String accountId, String appId, String envId, EnvironmentType envType,
      PermissionAttribute permissionAttribute, List<Role> roles, UserRequestInfo userRequestInfo) {
    if (isEmpty(roles)) {
      return false;
    }
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
  public UserPermissionInfo getUserPermissionInfo(String accountId, User user, boolean cacheOnly) {
    Cache<String, UserPermissionInfo> cache = cacheManager.getUserPermissionInfoCache();
    if (cache == null) {
      if (cacheOnly) {
        return null;
      }
      logger.error("UserInfoCache is null. This should not happen. Fall back to DB");
      return getUserPermissionInfoFromDB(accountId, user);
    }

    String key = getCacheKey(accountId, user.getUuid());
    UserPermissionInfo value;
    try {
      value = cache.get(key);
      if (value == null) {
        if (cacheOnly) {
          return null;
        }

        value = getUserPermissionInfoFromDB(accountId, user);
        cache.put(key, value);
      }
      return value;
    } catch (Exception e) {
      logger.warn("Error in fetching user UserPermissionInfo from Cache for key:" + key, e);
    }

    // not found in cache. cache write through failed as well. rebuild anyway
    return getUserPermissionInfoFromDB(accountId, user);
  }

  @Override
  public UserRestrictionInfo getUserRestrictionInfo(
      String accountId, User user, UserPermissionInfo userPermissionInfo, boolean cacheOnly) {
    Cache<String, UserRestrictionInfo> cache = cacheManager.getUserRestrictionInfoCache();
    if (cache == null) {
      if (cacheOnly) {
        return null;
      }
      logger.error("UserInfoCache is null. This should not happen. Fall back to DB");
      return getUserRestrictionInfoFromDB(accountId, user, userPermissionInfo);
    }

    String key = getCacheKey(accountId, user.getUuid());
    UserRestrictionInfo value;
    try {
      value = cache.get(key);
      if (value == null) {
        if (cacheOnly) {
          return null;
        }
        value = getUserRestrictionInfoFromDB(accountId, user, userPermissionInfo);
        cache.put(key, value);
      }
      return value;
    } catch (Exception ignored) {
      logger.warn("Error in fetching user UserPermissionInfo from Cache for key:" + key, ignored);
    }

    // not found in cache. cache write through failed as well. rebuild anyway
    return getUserRestrictionInfoFromDB(accountId, user, userPermissionInfo);
  }

  private String getCacheKey(String accountId, String userId) {
    return accountId + "~" + userId;
  }

  private String getUserId(String cacheKey, String accountId) {
    if (isEmpty(cacheKey)) {
      return null;
    }
    String prefix = accountId + "~";
    return cacheKey.replace(prefix, "");
  }

  @Override
  public void evictUserPermissionAndRestrictionCacheForAccount(
      String accountId, boolean rebuildUserPermissionInfo, boolean rebuildUserRestrictionInfo) {
    evictAndRebuild(
        accountId, rebuildUserPermissionInfo, cacheManager.getUserPermissionInfoCache(), UserPermissionInfo.class);
    evictAndRebuild(
        accountId, rebuildUserRestrictionInfo, cacheManager.getUserRestrictionInfoCache(), UserRestrictionInfo.class);
    apiKeyService.evictAndRebuildPermissionsAndRestrictions(
        accountId, rebuildUserPermissionInfo || rebuildUserRestrictionInfo);
  }

  @Override
  public void evictUserPermissionCacheForAccount(String accountId, boolean rebuildUserPermissionInfo) {
    evictAndRebuild(
        accountId, rebuildUserPermissionInfo, cacheManager.getUserPermissionInfoCache(), UserPermissionInfo.class);
    apiKeyService.evictAndRebuildPermissions(accountId, rebuildUserPermissionInfo);
  }

  private <T> void evictAndRebuild(String accountId, boolean rebuild, Cache<String, T> cache, Class<T> infoClass) {
    Set<String> keys = new HashSet<>();
    if (cache != null) {
      cache.iterator().forEachRemaining(stringUserPermissionInfoEntry -> {
        String key = stringUserPermissionInfoEntry.getKey();
        if (isNotEmpty(key) && key.startsWith(accountId)) {
          keys.add(key);
        }
      });
      cache.removeAll(keys);

      if (rebuild) {
        executorService.submit(() -> keys.forEach(key -> {
          String userId = getUserId(key, accountId);
          if (userId == null) {
            return;
          }

          Cache<String, User> userCache = cacheManager.getUserCache();
          if (userCache == null) {
            return;
          }
          User user = userCache.get(userId);
          if (user == null) {
            return;
          }

          List<UserGroup> userGroups = getUserGroups(accountId, user);

          // This call gets the userPermissionInfo from cache, if present.
          UserPermissionInfo userPermissionInfo = getUserPermissionInfo(accountId, user, true);
          if (userPermissionInfo == null) {
            userPermissionInfo = authHandler.evaluateUserPermissionInfo(accountId, userGroups, user);
            cacheManager.getUserPermissionInfoCache().put(key, userPermissionInfo);
          }

          if (infoClass.getSimpleName().equals("UserRestrictionInfo")) {
            // This call reloads the cache from db, if some user request does that first, this simply makes sure the
            // cache is rebuilt.
            UserRestrictionInfo userRestrictionInfo = getUserRestrictionInfo(accountId, user, userPermissionInfo, true);
            if (userRestrictionInfo == null) {
              userRestrictionInfo = getUserRestrictionInfoFromDB(accountId, userPermissionInfo, userGroups);
              Cache<String, UserRestrictionInfo> userRestrictionInfoCache = cacheManager.getUserRestrictionInfoCache();
              if (userRestrictionInfoCache != null) {
                userRestrictionInfoCache.put(key, userRestrictionInfo);
              }
            }
          }
        }));
      }
    }
  }

  private <T> void removeFromCache(Cache<String, T> cache, String accountId, List<String> memberIds) {
    if (cache != null && isNotEmpty(memberIds)) {
      Set<String> keys = memberIds.stream().map(userId -> getCacheKey(accountId, userId)).collect(toSet());
      cache.removeAll(keys);
    }
  }

  @Override
  public void evictUserPermissionAndRestrictionCacheForAccounts(Set<String> accountIds, List<String> memberIds) {
    if (isEmpty(accountIds)) {
      return;
    }
    accountIds.forEach(accountId -> evictUserPermissionAndRestrictionCacheForAccount(accountId, memberIds));
  }

  @Override
  public void evictPermissionAndRestrictionCacheForUserGroup(UserGroup userGroup) {
    evictUserPermissionAndRestrictionCacheForAccount(userGroup.getAccountId(), userGroup.getMemberIds());
    apiKeyService.evictPermissionsAndRestrictionsForUserGroup(userGroup);
  }

  @Override
  public void evictUserPermissionAndRestrictionCacheForAccount(String accountId, List<String> memberIds) {
    Cache<String, UserPermissionInfo> userPermissionInfoCache = cacheManager.getUserPermissionInfoCache();
    removeFromCache(userPermissionInfoCache, accountId, memberIds);

    Cache<String, UserRestrictionInfo> userRestrictionscache = cacheManager.getUserRestrictionInfoCache();
    removeFromCache(userRestrictionscache, accountId, memberIds);
  }

  private UserPermissionInfo getUserPermissionInfoFromDB(String accountId, User user) {
    List<UserGroup> userGroups = getUserGroups(accountId, user);
    return authHandler.evaluateUserPermissionInfo(accountId, userGroups, user);
  }

  private List<UserGroup> getUserGroups(String accountId, User user) {
    List<UserGroup> userGroups = userGroupService.getUserGroupsByAccountId(accountId, user);

    if (isEmpty(userGroups) && !userService.isUserAssignedToAccount(user, accountId)) {
      // Check if its a harness user
      Optional<UserGroup> harnessUserGroup = getHarnessUserGroupsByAccountId(accountId, user);
      if (harnessUserGroup.isPresent()) {
        userGroups = Lists.newArrayList(harnessUserGroup.get());
      }
    }
    return userGroups;
  }

  private UserRestrictionInfo getUserRestrictionInfoFromDB(
      String accountId, User user, UserPermissionInfo userPermissionInfo) {
    List<UserGroup> userGroups = getUserGroups(accountId, user);
    return getUserRestrictionInfoFromDB(accountId, userPermissionInfo, userGroups);
  }

  @Override
  public UserRestrictionInfo getUserRestrictionInfoFromDB(
      String accountId, UserPermissionInfo userPermissionInfo, List<UserGroup> userGroupList) {
    UserRestrictionInfoBuilder userRestrictionInfoBuilder = UserRestrictionInfo.builder();

    // Restrictions for update permissions
    userRestrictionInfoBuilder.appEnvMapForUpdateAction(
        usageRestrictionsService.getAppEnvMapFromUserPermissions(accountId, userPermissionInfo, UPDATE));
    userRestrictionInfoBuilder.usageRestrictionsForUpdateAction(
        usageRestrictionsService.getUsageRestrictionsFromUserPermissions(UPDATE, userGroupList));

    // Restrictions for read permissions
    userRestrictionInfoBuilder.appEnvMapForReadAction(
        usageRestrictionsService.getAppEnvMapFromUserPermissions(accountId, userPermissionInfo, READ));
    userRestrictionInfoBuilder.usageRestrictionsForReadAction(
        usageRestrictionsService.getUsageRestrictionsFromUserPermissions(READ, userGroupList));

    return userRestrictionInfoBuilder.build();
  }

  private Optional<UserGroup> getHarnessUserGroupsByAccountId(String accountId, User user) {
    Set<Action> actions = harnessUserGroupService.listAllowedUserActionsForAccount(accountId, user.getUuid());
    if (isEmpty(actions)) {
      return Optional.empty();
    }

    AppPermission appPermission = AppPermission.builder()
                                      .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                      .permissionType(PermissionType.ALL_APP_ENTITIES)
                                      .actions(Sets.newHashSet(READ, UPDATE, DELETE, CREATE, EXECUTE))
                                      .build();

    AccountPermissions accountPermissions =
        AccountPermissions.builder()
            .permissions(Sets.newHashSet(USER_PERMISSION_READ, ACCOUNT_MANAGEMENT, USER_PERMISSION_MANAGEMENT,
                TEMPLATE_MANAGEMENT, APPLICATION_CREATE_DELETE, AUDIT_VIEWER))
            .build();
    UserGroup userGroup = UserGroup.builder()
                              .accountId(accountId)
                              .accountPermissions(accountPermissions)
                              .appPermissions(Sets.newHashSet(appPermission))
                              .build();
    return Optional.of(userGroup);
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

    if (CREATE == requiredAction) {
      if (requiredPermissionType == PermissionType.SERVICE) {
        return appPermissionSummary.isCanCreateService();
      } else if (requiredPermissionType == PermissionType.PROVISIONER) {
        return appPermissionSummary.isCanCreateProvisioner();
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
    } else if (requiredPermissionType == PermissionType.PROVISIONER) {
      actionEntityIdMap = appPermissionSummary.getProvisionerPermissions();
    } else if (requiredPermissionType == PermissionType.ENV) {
      Map<Action, Set<EnvInfo>> actionEnvPermissionsMap = appPermissionSummary.getEnvPermissions();
      if (isEmpty(actionEnvPermissionsMap)) {
        return false;
      }

      Set<EnvInfo> envInfoSet = actionEnvPermissionsMap.get(requiredAction);
      if (isEmpty(envInfoSet)) {
        return false;
      }

      Set<String> envIdSet = envInfoSet.stream().map(envInfo -> envInfo.getEnvId()).collect(toSet());
      return envIdSet.contains(entityId);

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

    if (isEmpty(actionEntityIdMap)) {
      return false;
    }

    Collection<String> entityIds = actionEntityIdMap.get(requiredAction);
    if (isEmpty(entityIds)) {
      return false;
    }

    return entityIds.contains(entityId);
  }

  private AuthToken verifyJWTToken(String token) {
    String jwtPasswordSecret = secretManager.getJWTSecret(JWT_CATEGORY.AUTH_SECRET);
    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
      JWTVerifier verifier = JWT.require(algorithm).withIssuer("Harness Inc").build();
      verifier.verify(token);
      String authToken = JWT.decode(token).getClaim("authToken").asString();
      return dbCache.get(AuthToken.class, authToken);
    } catch (UnsupportedEncodingException | JWTCreationException exception) {
      throw new WingsException(GENERAL_ERROR, exception).addParam("message", "JWTToken validation failed");
    } catch (JWTDecodeException | SignatureVerificationException | InvalidClaimException e) {
      throw new WingsException(INVALID_CREDENTIAL, USER, e)
          .addParam("message", "Invalid JWTToken received, failed to decode the token");
    }
  }

  @Override
  public User refreshToken(String oldToken) {
    if (oldToken.length() <= 32) {
      AuthToken authToken = dbCache.get(AuthToken.class, oldToken);
      if (authToken == null) {
        throw new WingsException(EXPIRED_TOKEN, USER);
      }

      User user = getUserFromAuthToken(authToken);
      user.setToken(authToken.getUuid());
      return user;
    }

    AuthToken authToken = verifyToken(oldToken);
    if (authToken.isRefreshed()) {
      throw new WingsException(TOKEN_ALREADY_REFRESHED_ONCE, USER);
    }
    User user = getUserFromAuthToken(authToken);
    authToken.setRefreshed(true);
    persistence.save(authToken);
    return generateBearerTokenForUser(user);
  }

  private User getUserFromAuthToken(AuthToken authToken) {
    User user = getUserFromCacheOrDB(authToken.getUserId());
    if (user == null) {
      logger.warn("No user found for userId:" + authToken.getUserId());
      throw new WingsException(USER_DOES_NOT_EXIST, USER);
    }
    return user;
  }

  @Override
  public User generateBearerTokenForUser(User user) {
    String acctId = user == null ? null : user.getDefaultAccountId();
    String uuid = user == null ? null : user.getUuid();
    try (AutoLogContext ignore = new UserLogContext(acctId, uuid, OVERRIDE_ERROR)) {
      logger.info("Generating bearer token");
      AuthToken authToken = new AuthToken(
          user.getLastAccountId(), user.getUuid(), configuration.getPortal().getAuthTokenExpiryInMillis());
      authToken.setJwtToken(generateJWTSecret(authToken));
      persistence.save(authToken);
      boolean isFirstLogin = user.getLastLogin() == 0L;
      user.setLastLogin(System.currentTimeMillis());
      userService.update(user);

      userService.evictUserFromCache(user.getUuid());
      user.setToken(authToken.getJwtToken());

      user.setFirstLogin(isFirstLogin);
      if (!user.getEmail().endsWith(Keys.HARNESS_EMAIL)) {
        executorService.submit(() -> {
          String accountId = user.getLastAccountId();
          if (isEmpty(accountId)) {
            logger.warn("last accountId is null for User {}", user.getUuid());
            return;
          }

          Account account = dbCache.get(Account.class, accountId);
          if (account == null) {
            logger.warn("last account is null for User {}", user.getUuid());
            return;
          }
          try {
            if (segmentHandler != null) {
              Map<String, String> properties = new HashMap<>();
              properties.put(SegmentHandler.Keys.GROUP_ID, accountId);

              Map<String, Boolean> integrations = new HashMap<>();
              integrations.put(SegmentHandler.Keys.NATERO, true);
              integrations.put(SegmentHandler.Keys.SALESFORCE, false);

              segmentHandler.reportTrackEvent(account, Keys.LOGIN_EVENT, user, properties, integrations);
            }
          } catch (Exception e) {
            logger.error("Exception while reporting track event for User {} login", user.getUuid(), e);
          }
        });
      }

      return user;
    }
  }

  private String generateJWTSecret(AuthToken authToken) {
    String jwtAuthSecret = secretManager.getJWTSecret(JWT_CATEGORY.AUTH_SECRET);
    int duration = JWT_CATEGORY.AUTH_SECRET.getValidityDuration();
    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtAuthSecret);
      return JWT.create()
          .withIssuer("Harness Inc")
          .withIssuedAt(new Date())
          .withExpiresAt(new Date(System.currentTimeMillis() + duration))
          .withClaim("authToken", authToken.getUuid())
          .withClaim("usrId", authToken.getUserId())
          .withClaim("env", configuration.getEnvPath())
          .sign(algorithm);
    } catch (UnsupportedEncodingException | JWTCreationException exception) {
      throw new WingsException(GENERAL_ERROR, exception).addParam("message", "JWTToken could not be generated");
    }
  }

  @Override
  public void deleteByAccountId(String accountId) {
    whitelistService.deleteByAccountId(accountId);
    ssoSettingService.deleteByAccountId(accountId);
    userService.deleteByAccountId(accountId);
    userGroupService.deleteByAccountId(accountId);
  }

  @Override
  public void checkIfUserAllowedToDeployToEnv(String appId, String envId) {
    if (isEmpty(envId)) {
      return;
    }

    User user = UserThreadLocal.get();
    if (user == null) {
      return;
    }

    Set<String> deploymentEnvExecutePermissions = user.getUserRequestContext()
                                                      .getUserPermissionInfo()
                                                      .getAppPermissionMapInternal()
                                                      .get(appId)
                                                      .getDeploymentExecutePermissionsForEnvs();
    if (isEmpty(deploymentEnvExecutePermissions)) {
      throw new WingsException(ErrorCode.ACCESS_DENIED, USER);
    }

    if (!deploymentEnvExecutePermissions.contains(envId)) {
      throw new WingsException(ErrorCode.ACCESS_DENIED, USER);
    }
  }

  @Override
  public void checkIfUserCanCreateEnv(String appId, EnvironmentType envType) {
    User user = UserThreadLocal.get();
    if (user == null) {
      return;
    }

    if (envType == null) {
      throw new WingsException("No environment type specified", USER);
    }

    Set<EnvironmentType> envCreatePermissions = user.getUserRequestContext()
                                                    .getUserPermissionInfo()
                                                    .getAppPermissionMapInternal()
                                                    .get(appId)
                                                    .getEnvCreatePermissionsForEnvTypes();

    if (isEmpty(envCreatePermissions)) {
      throw new WingsException(ErrorCode.ACCESS_DENIED, USER);
    }

    if (envCreatePermissions.contains(EnvironmentType.ALL)) {
      return;
    }

    if (!envCreatePermissions.contains(envType)) {
      throw new WingsException(ErrorCode.ACCESS_DENIED, USER);
    }
  }

  @Override
  public void checkWorkflowPermissionsForEnv(String appId, Workflow workflow, Action action) {
    if (workflow == null) {
      return;
    }
    boolean envTemplatized = authHandler.isEnvTemplatized(workflow);
    String envId = workflow.getEnvId();

    if (!envTemplatized && isEmpty(envId)) {
      return;
    }

    User user = UserThreadLocal.get();
    if (user == null) {
      return;
    }

    AppPermissionSummary appPermissionSummary =
        user.getUserRequestContext().getUserPermissionInfo().getAppPermissionMapInternal().get(appId);
    if (envTemplatized) {
      if (appPermissionSummary.isCanCreateTemplatizedWorkflow()) {
        return;
      } else {
        throw new WingsException(ErrorCode.ACCESS_DENIED, USER);
      }
    }

    Set<String> allowedEnvIds;
    switch (action) {
      case CREATE:
        allowedEnvIds = appPermissionSummary.getWorkflowCreatePermissionsForEnvs();
        break;
      case UPDATE:
        allowedEnvIds = appPermissionSummary.getWorkflowUpdatePermissionsForEnvs();
        break;
      default:
        return;
    }

    if (isEmpty(allowedEnvIds)) {
      throw new WingsException(ErrorCode.ACCESS_DENIED, USER);
    }
    if (!allowedEnvIds.contains(envId)) {
      throw new WingsException(ErrorCode.ACCESS_DENIED, USER);
    }
  }

  @Override
  public void checkIfUserCanCloneWorkflowToOtherApp(String targetAppId, Workflow workflow) {
    if (workflow == null) {
      return;
    }
    boolean envTemplatized = authHandler.isEnvTemplatized(workflow);

    if (!envTemplatized) {
      return;
    }

    User user = UserThreadLocal.get();
    if (user == null) {
      return;
    }

    AppPermissionSummary appPermissionSummary =
        user.getUserRequestContext().getUserPermissionInfo().getAppPermissionMapInternal().get(targetAppId);

    if (!appPermissionSummary.isCanCreateTemplatizedWorkflow()) {
      throw new WingsException(ErrorCode.ACCESS_DENIED, USER);
    }
  }

  @Override
  public void checkPipelinePermissionsForEnv(String appId, Pipeline pipeline, Action action) {
    User user = UserThreadLocal.get();

    if (user == null) {
      return;
    }

    AppPermissionSummary appPermissionSummary =
        user.getUserRequestContext().getUserPermissionInfo().getAppPermissionMapInternal().get(appId);
    Set<String> allowedEnvIds;

    switch (action) {
      case CREATE:
        allowedEnvIds = appPermissionSummary.getPipelineCreatePermissionsForEnvs();
        break;
      case UPDATE:
        allowedEnvIds = appPermissionSummary.getPipelineUpdatePermissionsForEnvs();
        break;
      default:
        return;
    }

    if (!authHandler.checkIfPipelineHasOnlyGivenEnvs(pipeline, allowedEnvIds)) {
      throw new WingsException(ErrorCode.ACCESS_DENIED, USER);
    }
  }

  @Override
  public void authorizeAppAccess(String accountId, String appId, User user, Action action) {
    UserPermissionInfo userPermissionInfo = authorizeAndGetUserPermissionInfo(accountId, appId, user, false);

    Map<String, AppPermissionSummaryForUI> appPermissionMap = userPermissionInfo.getAppPermissionMap();
    if (appPermissionMap == null || !appPermissionMap.containsKey(appId)) {
      logger.error("Auth Failure: User does not have access to app {}", appId);
      throw new WingsException(ACCESS_DENIED, USER);
    }

    if (Action.UPDATE.equals(action)) {
      AccountPermissionSummary accountPermissionSummary = userPermissionInfo.getAccountPermissionSummary();

      if (accountPermissionSummary == null || isEmpty(accountPermissionSummary.getPermissions())
          || !accountPermissionSummary.getPermissions().contains(APPLICATION_CREATE_DELETE)) {
        logger.error("Auth Failure: User does not have access to update {}", appId);
        throw new WingsException(ACCESS_DENIED, USER);
      }
    }
  }
}
