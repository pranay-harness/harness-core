package software.wings.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.security.AuthRuleFilter.getAllowedAppIds;

import static java.util.Arrays.asList;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.security.annotations.DelegateAuth;
import io.harness.security.annotations.LearningEngineAuth;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.annotations.PublicApi;
import io.harness.security.annotations.PublicApiWithWhitelist;

import software.wings.beans.User;
import software.wings.security.UserRequestContext.UserRequestContextBuilder;
import software.wings.security.annotations.AdminPortalAuth;
import software.wings.security.annotations.ApiKeyAuthorized;
import software.wings.security.annotations.ExternalFacingApiAuth;
import software.wings.security.annotations.ScimAPI;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AuthService;

import com.google.inject.Inject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.ws.rs.container.ResourceInfo;
import lombok.Setter;

public class AuthHelper {
  @Setter private ResourceInfo resourceInfo;

  private AuthHandler authHandler;
  private AuthService authService;

  @Inject
  public AuthHelper(AuthHandler authHandler, AuthService authService) {
    this.authHandler = authHandler;
    this.authService = authService;
  }

  public UserRequestContext buildUserRequestContext(List<PermissionAttribute> requiredPermissionAttributes, User user,
      String accountId, boolean emptyAppIdsInReq, String httpMethod, List<String> appIdsFromRequest, boolean skipAuth,
      boolean accountLevelPermissions, boolean harnessSupportUser) {
    UserRequestContext userRequestContext =
        buildUserRequestContext(accountId, user, emptyAppIdsInReq, harnessSupportUser);

    if (!accountLevelPermissions) {
      authHandler.setEntityIdFilterIfGet(httpMethod, skipAuth, requiredPermissionAttributes, userRequestContext,
          userRequestContext.isAppIdFilterRequired(), userRequestContext.getAppIds(), appIdsFromRequest);
    }
    return userRequestContext;
  }

  public UserRequestContext buildUserRequestContext(
      String accountId, User user, boolean emptyAppIdsInReq, boolean harnessSupportUser) {
    UserRequestContextBuilder userRequestContextBuilder =
        UserRequestContext.builder().accountId(accountId).entityInfoMap(new HashMap<>());

    UserPermissionInfo userPermissionInfo = authService.getUserPermissionInfo(accountId, user, false);
    userRequestContextBuilder.userPermissionInfo(userPermissionInfo);

    UserRestrictionInfo userRestrictionInfo =
        authService.getUserRestrictionInfo(accountId, user, userPermissionInfo, false);
    userRequestContextBuilder.userRestrictionInfo(userRestrictionInfo);

    Set<String> allowedAppIds = getAllowedAppIds(userPermissionInfo);
    setAppIdFilterInUserRequestContext(userRequestContextBuilder, emptyAppIdsInReq, allowedAppIds);

    userRequestContextBuilder.harnessSupportUser(harnessSupportUser);
    return userRequestContextBuilder.build();
  }

  private boolean setAppIdFilterInUserRequestContext(
      UserRequestContextBuilder userRequestContextBuilder, boolean emptyAppIdsInReq, Set<String> allowedAppIds) {
    if (!emptyAppIdsInReq) {
      return false;
    }

    List<PermissionAttribute.ResourceType> requiredResourceTypes = getAllResourceTypes();
    if (isPresent(requiredResourceTypes, PermissionAttribute.ResourceType.APPLICATION)) {
      userRequestContextBuilder.appIdFilterRequired(true);
      userRequestContextBuilder.appIds(allowedAppIds);
      return true;
    }

    return false;
  }

  private boolean isPresent(
      List<PermissionAttribute.ResourceType> requiredResourceTypes, PermissionAttribute.ResourceType resourceType) {
    return requiredResourceTypes.stream().anyMatch(requiredResourceType -> requiredResourceType == resourceType);
  }

  private List<PermissionAttribute.ResourceType> getAllResourceTypes() {
    List<PermissionAttribute.ResourceType> methodResourceTypes = new ArrayList<>();
    List<PermissionAttribute.ResourceType> classResourceTypes = new ArrayList<>();

    Method resourceMethod = resourceInfo.getResourceMethod();
    Scope methodAnnotations = resourceMethod.getAnnotation(Scope.class);
    if (null != methodAnnotations) {
      methodResourceTypes = asList(methodAnnotations.value());
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    Scope classAnnotations = resourceClass.getAnnotation(Scope.class);
    if (null != classAnnotations) {
      classResourceTypes = asList(classAnnotations.value());
    }

    if (methodResourceTypes.isEmpty()) {
      return classResourceTypes;
    } else {
      return methodResourceTypes;
    }
  }

  public UserRequestContext buildUserRequestContext(UserPermissionInfo userPermissionInfo,
      UserRestrictionInfo userRestrictionInfo, List<PermissionAttribute> requiredPermissionAttributes, String accountId,
      boolean emptyAppIdsInReq, String httpMethod, List<String> appIdsFromRequest, boolean skipAuth,
      boolean accountLevelPermissions, boolean isScopeToApp) {
    UserRequestContext userRequestContext = buildUserRequestContext(
        userPermissionInfo, userRestrictionInfo, accountId, emptyAppIdsInReq, isScopeToApp, appIdsFromRequest);

    if (!accountLevelPermissions) {
      authHandler.setEntityIdFilterIfGet(httpMethod, skipAuth, requiredPermissionAttributes, userRequestContext,
          userRequestContext.isAppIdFilterRequired(), userRequestContext.getAppIds(), appIdsFromRequest);
    }
    return userRequestContext;
  }

  public UserRequestContext buildUserRequestContext(UserPermissionInfo userPermissionInfo,
      UserRestrictionInfo userRestrictionInfo, String accountId, boolean emptyAppIdsInReq, boolean isScopedToApp,
      List<String> appIdsFromRequest) {
    UserRequestContextBuilder userRequestContextBuilder =
        UserRequestContext.builder().accountId(accountId).entityInfoMap(new HashMap<>());

    userRequestContextBuilder.userPermissionInfo(userPermissionInfo);
    userRequestContextBuilder.userRestrictionInfo(userRestrictionInfo);

    if (isScopedToApp) {
      Set<String> allowedAppIds = getAllowedAppIds(userPermissionInfo);
      if (emptyAppIdsInReq) {
        userRequestContextBuilder.appIdFilterRequired(true);
        userRequestContextBuilder.appIds(allowedAppIds);
      } else {
        if (isEmpty(allowedAppIds) || !allowedAppIds.containsAll(appIdsFromRequest)) {
          throw new WingsException(ErrorCode.ACCESS_DENIED);
        }
      }
    }

    return userRequestContextBuilder.build();
  }

  protected boolean delegateAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(DelegateAuth.class) != null
        || resourceClass.getAnnotation(DelegateAuth.class) != null;
  }

  protected boolean learningEngineServiceAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(LearningEngineAuth.class) != null
        || resourceClass.getAnnotation(LearningEngineAuth.class) != null;
  }

  protected boolean externalAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(ExternalFacingApiAuth.class) != null
        || resourceClass.getAnnotation(ExternalFacingApiAuth.class) != null;
  }

  protected boolean publicAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(PublicApi.class) != null
        || resourceClass.getAnnotation(PublicApi.class) != null;
  }

  protected boolean isScimAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(ScimAPI.class) != null || resourceClass.getAnnotation(ScimAPI.class) != null;
  }

  protected boolean adminPortalAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();
    return resourceMethod.getAnnotation(AdminPortalAuth.class) != null
        || resourceClass.getAnnotation(AdminPortalAuth.class) != null;
  }

  protected boolean isNextGenManagerAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();
    return resourceMethod.getAnnotation(NextGenManagerAuth.class) != null
        || resourceClass.getAnnotation(NextGenManagerAuth.class) != null;
  }

  protected boolean isPublicApiWithWhitelist() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();
    return resourceMethod.getAnnotation(PublicApiWithWhitelist.class) != null
        || resourceClass.getAnnotation(PublicApiWithWhitelist.class) != null;
  }

  protected boolean apiKeyAuthorizationAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(ApiKeyAuthorized.class) != null
        || resourceClass.getAnnotation(ApiKeyAuthorized.class) != null;
  }
}
