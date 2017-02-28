package software.wings.service.impl;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.ErrorCode.ACCESS_DENIED;
import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.beans.ErrorCode.EXPIRED_TOKEN;
import static software.wings.beans.ErrorCode.INVALID_TOKEN;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.AuthToken;
import software.wings.beans.Environment;
import software.wings.beans.Permission;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.GenericDbCache;
import software.wings.dl.PageRequest.PageRequestType;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionScope;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;

import java.text.ParseException;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 8/18/16.
 */
@Singleton
public class AuthServiceImpl implements AuthService {
  private final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
  private GenericDbCache dbCache;

  private AccountService accountService;

  /**
   * Instantiates a new Auth service.
   *
   * @param dbCache the db cache
   */
  @Inject
  public AuthServiceImpl(GenericDbCache dbCache, AccountService accountService) {
    this.dbCache = dbCache;
    this.accountService = accountService;
  }

  @Override
  public AuthToken validateToken(String tokenString) {
    AuthToken authToken = dbCache.get(AuthToken.class, tokenString);
    if (authToken == null) {
      throw new WingsException(INVALID_TOKEN);
    } else if (authToken.getExpireAt() <= System.currentTimeMillis()) {
      throw new WingsException(EXPIRED_TOKEN);
    }
    return authToken;
  }

  @Override
  public void authorize(String appId, String envId, User user, List<PermissionAttribute> permissionAttributes,
      PageRequestType requestType) {
    if (user.isAdmin()) {
      return;
    }
    for (PermissionAttribute permissionAttribute : permissionAttributes) {
      if (!authorizeAccessType(appId, envId, permissionAttribute, user.getRoles(), requestType)) {
        throw new WingsException(ACCESS_DENIED);
      }
    }
  }

  @Override
  public void validateDelegateToken(String accountId, String tokenString) {
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

  private boolean authorizeAccessType(String appId, String envId, PermissionAttribute permissionAttribute,
      List<Role> roles, PageRequestType requestType) {
    return roles.stream()
        .filter(role -> roleAuthorizedWithAccessType(role, permissionAttribute, appId, envId, requestType))
        .findFirst()
        .isPresent();
  }

  private boolean roleAuthorizedWithAccessType(
      Role role, PermissionAttribute permissionAttribute, String appId, String envId, PageRequestType requestType) {
    if (role.isAdminRole()) {
      return true;
    }
    ResourceType reqResourceType = permissionAttribute.getResourceType();
    Action reqAction = permissionAttribute.getAction();
    boolean requiresEnvironmentPermission = permissionAttribute.getScope().equals(PermissionScope.ENV);
    for (Permission permission : role.getPermissions()) {
      if (hasMatchingPermissionType(requiresEnvironmentPermission, permission.getPermissionScope())
          && hasResourceAccess(reqResourceType, permission) && canPerformAction(reqAction, permission)
          && allowedInEnv(envId, requiresEnvironmentPermission, permission, requestType)
          && forApplication(appId, permission, requestType)) {
        return true;
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
    return Action.ALL.equals(permission.getAction()) || (reqAction.equals(permission.getAction()));
  }

  private boolean hasResourceAccess(ResourceType reqResource, Permission permission) {
    return ResourceType.ANY.equals(permission.getResourceType()) || (reqResource.equals(permission.getResourceType()));
  }
}
