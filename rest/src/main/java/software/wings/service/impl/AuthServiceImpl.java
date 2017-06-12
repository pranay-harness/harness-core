package software.wings.service.impl;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.ErrorCode.ACCESS_DENIED;
import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.beans.ErrorCode.EXPIRED_TOKEN;
import static software.wings.beans.ErrorCode.INVALID_TOKEN;
import static software.wings.beans.ErrorCode.USER_DOES_NOT_EXIST;
import static software.wings.dl.PageRequest.PageRequestType.LIST_WITHOUT_APP_ID;
import static software.wings.dl.PageRequest.PageRequestType.LIST_WITHOUT_ENV_ID;

import com.google.inject.Singleton;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jwt.EncryptedJWT;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.mongodb.morphia.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.AuthToken;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Permission;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.GenericDbCache;
import software.wings.dl.PageRequest.PageRequestType;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionScope;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.UserRequestInfo;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserService;
import software.wings.utils.CacheHelper;

import java.text.ParseException;
import java.util.List;
import javax.cache.Cache;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 8/18/16.
 */
@Singleton
public class AuthServiceImpl implements AuthService {
  private GenericDbCache dbCache;
  private AccountService accountService;
  private WingsPersistence wingsPersistence;
  private UserService userService;
  private CacheHelper cacheHelper;

  private final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

  /**
   * Instantiates a new Auth service.
   *
   * @param dbCache          the db cache
   * @param accountService   the account service
   * @param wingsPersistence the wings persistence
   * @param userService      the user service
   * @param cacheHelper      the cache helper
   */
  @Inject
  public AuthServiceImpl(GenericDbCache dbCache, AccountService accountService, WingsPersistence wingsPersistence,
      UserService userService, CacheHelper cacheHelper) {
    this.dbCache = dbCache;
    this.accountService = accountService;
    this.wingsPersistence = wingsPersistence;
    this.userService = userService;
    this.cacheHelper = cacheHelper;
  }

  @Override
  public AuthToken validateToken(String tokenString) {
    AuthToken authToken = dbCache.get(AuthToken.class, tokenString);

    if (authToken == null) {
      throw new WingsException(INVALID_TOKEN);
    } else if (authToken.getExpireAt() <= System.currentTimeMillis()) {
      throw new WingsException(EXPIRED_TOKEN);
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
      User user = userCache.get(authToken.getUserId());
      if (user == null) {
        user = userService.get(authToken.getUserId());
        userCache.put(user.getUuid(), user);
      }
      return user;
    }
  }

  @Override
  public void authorize(String accountId, String appId, String envId, User user,
      List<PermissionAttribute> permissionAttributes, UserRequestInfo userRequestInfo) {
    if (accountId == null || dbCache.get(Account.class, accountId) == null) {
      logger.error("Auth Failure: non-existing accountId: {}", accountId);
      throw new WingsException(ACCESS_DENIED);
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
  public void validateDelegateToken(String accountId, String tokenString) {
    logger.info("Delegate token validation, account id [{}] token [{}]", accountId, tokenString); // TODO: remove this
    Account account = accountService.get(accountId);
    if (account == null) {
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
  public void invalidateAllTokensForUser(String userId) {
    List<Key<AuthToken>> keyList =
        wingsPersistence.createQuery(AuthToken.class).field("user").equal(userId).asKeyList();
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

    ResourceType reqResourceType = permissionAttribute.getResourceType();
    Action reqAction = permissionAttribute.getAction();
    PermissionScope permissionScope = permissionAttribute.getScope();

    for (Permission permission : role.getPermissions()) {
      if (permission.getPermissionScope() != permissionScope
          || (permission.getAction() != Action.ALL && reqAction != permission.getAction())) {
        continue;
      }
      if (permissionScope == PermissionScope.APP) {
        if (userRequestInfo != null
            && (userRequestInfo.isAllAppsAllowed() || userRequestInfo.getAllowedAppIds().contains(appId))) {
          return true;
        }
        if (permission.getAppId() != null
            && (permission.getAppId().equals(GLOBAL_APP_ID) || permission.getAppId().equals(appId))) {
          return true;
        }
      } else if (permissionScope == PermissionScope.ENV) {
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

  private boolean hasMatchingPermissionType(boolean requiresEnvironmentPermission, PermissionScope permissionScope) {
    return requiresEnvironmentPermission ? permissionScope.equals(PermissionScope.ENV)
                                         : permissionScope.equals(PermissionScope.APP);
  }

  private boolean forApplication(String appId, Permission permission, PageRequestType requestType) {
    return requestType.equals(LIST_WITHOUT_APP_ID) || GLOBAL_APP_ID.equals(permission.getAppId())
        || (appId != null && appId.equals(permission.getAppId()));
  }

  private boolean allowedInEnv(
      String envId, boolean requiresEnvironmentPermission, Permission permission, PageRequestType requestType) {
    return !requiresEnvironmentPermission || requestType.equals(LIST_WITHOUT_ENV_ID)
        || allowedInSpecificEnvironment(envId, permission);
  }

  private boolean allowedInSpecificEnvironment(String envId, Permission permission) {
    if (envId != null) {
      Environment environment = dbCache.get(Environment.class, envId);
      return hasAccessByEnvType(environment, permission) || hasAccessByEnvId(environment, permission);
    } else {
      return hasAccessByEnvType(null, permission);
    }
  }

  private boolean hasAccessByEnvId(Environment environment, Permission permission) {
    return GLOBAL_ENV_ID.equals(permission.getEnvId())
        || (environment != null && environment.getUuid().equals(permission.getEnvId()));
  }

  private boolean hasAccessByEnvType(Environment environment, Permission permission) {
    return ALL.equals(permission.getEnvironmentType())
        || (environment != null && environment.getEnvironmentType().equals(permission.getEnvironmentType()));
  }

  private boolean canPerformAction(Action reqAction, Permission permission) {
    return reqAction.equals(permission.getAction());
  }

  private boolean hasResourceAccess(ResourceType reqResource, Permission permission) {
    return reqResource.equals(permission.getResourceType());
  }
}
