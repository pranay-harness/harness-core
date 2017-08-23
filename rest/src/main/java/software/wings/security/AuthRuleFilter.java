package software.wings.security;

import static com.google.common.collect.ImmutableList.copyOf;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.ErrorCode.ACCESS_DENIED;
import static software.wings.beans.ErrorCode.INVALID_TOKEN;
import static software.wings.security.UserRequestInfo.UserRequestInfoBuilder.anUserRequestInfo;

import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AccountRole;
import software.wings.beans.ApplicationRole;
import software.wings.beans.AuthToken;
import software.wings.beans.EnvironmentRole;
import software.wings.beans.ErrorCode;
import software.wings.beans.Permission;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.common.AuditHelper;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionScope;
import software.wings.security.UserRequestInfo.UserRequestInfoBuilder;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.ExternalServiceAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by anubhaw on 3/11/16.
 */
@Singleton
@Priority(AUTHENTICATION)
public class AuthRuleFilter implements ContainerRequestFilter {
  private final Logger logger = LoggerFactory.getLogger(AuthRuleFilter.class);

  @Context private ResourceInfo resourceInfo;

  private AuditService auditService;
  private AuditHelper auditHelper;
  private AuthService authService;
  private UserService userService;
  private AppService appService;

  /**
   * Instantiates a new Auth rule filter.
   *
   * @param auditService the audit service
   * @param auditHelper  the audit helper
   * @param authService  the auth service
   * @param appService   the appService
   * @param userService  the userService
   */
  @Inject
  public AuthRuleFilter(AuditService auditService, AuditHelper auditHelper, AuthService authService,
      AppService appService, UserService userService) {
    this.auditService = auditService;
    this.auditHelper = auditHelper;
    this.authService = authService;
    this.appService = appService;
    this.userService = userService;
  }

  /* (non-Javadoc)
   * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
   */
  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (authorizationExemptedRequest(requestContext)) {
      return; // do nothing
    }
    List<PermissionAttribute> requiredPermissionAttributes = getAllRequiredPermissionAttributes(requestContext);
    if (requiredPermissionAttributes == null || requiredPermissionAttributes.isEmpty()) {
      logger.error("Requested Resource: {}", requestContext.getUriInfo().getPath());
      throw new WingsException(ACCESS_DENIED);
    }

    MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
    MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();

    if (isDelegateRequest(requestContext) && isExternalServiceRequest(requestContext)) {
      String accountId = getRequestParamFromContext("accountId", pathParameters, queryParameters);
      String header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
      if (header.contains("Delegate")) {
        authService.validateDelegateToken(
            accountId, substringAfter(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "Delegate "));
      } else if (header.contains("ExternalService")) {
        authService.validateExternalServiceToken(
            accountId, substringAfter(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "ExternalService "));
      } else {
        throw new IllegalStateException("Invalid header:" + header);
      }

      return;
    }

    if (isDelegateRequest(requestContext)) {
      String accountId = getRequestParamFromContext("accountId", pathParameters, queryParameters);
      authService.validateDelegateToken(
          accountId, substringAfter(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "Delegate "));
      return; // do nothing
    }

    if (isExternalServiceRequest(requestContext)) {
      String accountId = getRequestParamFromContext("accountId", pathParameters, queryParameters);
      authService.validateExternalServiceToken(
          accountId, substringAfter(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "ExternalService "));
      return; // do nothing
    }

    String tokenString = extractToken(requestContext);
    AuthToken authToken = authService.validateToken(tokenString);
    User user = authToken.getUser();
    if (user != null) {
      logger.info("User: {}", user);
      requestContext.setProperty("USER", user);
      updateUserInAuditRecord(user); // FIXME: find better place
      UserThreadLocal.set(user);
    }

    if (allLoggedInScope(requiredPermissionAttributes)) {
      return;
    }

    String accountId = getRequestParamFromContext("accountId", pathParameters, queryParameters);
    String appId = getRequestParamFromContext("appId", pathParameters, queryParameters);
    String envId = getRequestParamFromContext("envId", pathParameters, queryParameters);

    if (appId != null && accountId == null) {
      accountId = appService.get(appId).getAccountId();
    }

    if (accountId == null) {
      if (appId == null) {
        throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "appId not specified");
      }
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "accountId not specified");
    }

    UserRequestInfoBuilder userRequestInfoBuilder =
        anUserRequestInfo().withAccountId(accountId).withAppId(appId).withEnvId(envId).withPermissionAttributes(
            ImmutableList.copyOf(requiredPermissionAttributes));

    if (user.isAccountAdmin(accountId) || user.isAllAppAdmin(accountId)) {
      userRequestInfoBuilder.withAllAppsAllowed(true).withAllEnvironmentsAllowed(true);
      if (appId == null && isPresent(requiredPermissionAttributes, PermissionScope.APP)) {
        userRequestInfoBuilder.withAppIdFilterRequired(true).withAllowedAppIds(
            ImmutableList.copyOf(appService.getAppIdsByAccountId(accountId)));
      }
    } else {
      // TODO:
      logger.info("User {} is neither account admin nor all app admin", user.getName());
      AccountRole userAccountRole = userService.getUserAccountRole(user.getUuid(), accountId);
      if (userAccountRole == null) {
        logger.info("No account role exist for user {}", user.getName());
      }
      logger.info("User account role {}", userAccountRole);
      ImmutableList<String> appIds = copyOf(userAccountRole.getApplicationRoles()
                                                .stream()
                                                .map(ApplicationRole::getAppId)
                                                .distinct()
                                                .collect(Collectors.toList()));

      if (appId != null) {
        if (user.isAppAdmin(accountId, appId)) {
          userRequestInfoBuilder.withAllEnvironmentsAllowed(true);
        } else {
          ApplicationRole applicationRole = userService.getUserApplicationRole(user.getUuid(), appId);
          ImmutableList<String> envIds = copyOf(applicationRole.getEnvironmentRoles()
                                                    .stream()
                                                    .map(EnvironmentRole::getEnvId)
                                                    .distinct()
                                                    .collect(Collectors.toList()));
          userRequestInfoBuilder.withAllEnvironmentsAllowed(false).withAllowedEnvIds(envIds);
        }
      }

      userRequestInfoBuilder.withAllAppsAllowed(false).withAllowedAppIds(appIds);
    }

    UserRequestInfo userRequestInfo = userRequestInfoBuilder.build();
    user.setUserRequestInfo(userRequestInfo);

    authService.authorize(accountId, appId, envId, user, requiredPermissionAttributes, userRequestInfo);
  }

  private boolean isPresent(List<PermissionAttribute> requiredPermissionAttributes, PermissionScope permissionScope) {
    for (PermissionAttribute permissionAttribute : requiredPermissionAttributes) {
      if (permissionAttribute.getScope() == permissionScope) {
        return true;
      }
    }
    return false;
  }

  private boolean allLoggedInScope(List<PermissionAttribute> requiredPermissionAttributes) {
    return !requiredPermissionAttributes.parallelStream()
                .filter(permissionAttribute -> permissionAttribute.getScope() != PermissionScope.LOGGED_IN)
                .findFirst()
                .isPresent();
  }

  private boolean isDelegateRequest(ContainerRequestContext requestContext) {
    return delegateAPI() && startsWith(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "Delegate ");
  }

  private boolean isExternalServiceRequest(ContainerRequestContext requestContext) {
    return externalServiceAPI()
        && startsWith(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "ExternalService ");
  }

  private boolean authorizationExemptedRequest(ContainerRequestContext requestContext) {
    return publicAPI() || requestContext.getMethod().equals(OPTIONS)
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/version")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/swagger")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/swagger.json");
  }

  private String getRequestParamFromContext(
      String key, MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
    return queryParameters.getFirst(key) != null ? queryParameters.getFirst(key) : pathParameters.getFirst(key);
  }

  private boolean publicAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(PublicApi.class) != null
        || resourceClass.getAnnotation(PublicApi.class) != null;
  }

  private boolean delegateAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(DelegateAuth.class) != null
        || resourceClass.getAnnotation(DelegateAuth.class) != null;
  }

  private boolean externalServiceAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(ExternalServiceAuth.class) != null
        || resourceClass.getAnnotation(ExternalServiceAuth.class) != null;
  }

  private List<String> getAppIds(List<Role> roles) {
    return roles.stream()
        .flatMap(role -> role.getPermissions().stream())
        .filter(permission
            -> permission.getPermissionScope() == PermissionScope.APP && permission.getAction() == Action.READ
                && permission.getAppId() != null && !permission.getAppId().equals(GLOBAL_APP_ID))
        .map(Permission::getAppId)
        .distinct()
        .collect(Collectors.toList());
  }

  private List<PermissionAttribute> getAllRequiredPermissionAttributes(ContainerRequestContext requestContext) {
    List<PermissionAttribute> methodPermissionAttributes = new ArrayList<>();
    List<PermissionAttribute> classPermissionAttributes = new ArrayList<>();

    Method resourceMethod = resourceInfo.getResourceMethod();
    AuthRule methodAnnotations = resourceMethod.getAnnotation(AuthRule.class);
    if (null != methodAnnotations) {
      Stream.of(methodAnnotations.value())
          .forEach(s
              -> methodPermissionAttributes.add(
                  new PermissionAttribute(s, methodAnnotations.scope(), requestContext.getMethod())));
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    AuthRule classAnnotations = resourceClass.getAnnotation(AuthRule.class);
    if (null != classAnnotations) {
      Stream.of(classAnnotations.value())
          .forEach(s
              -> classPermissionAttributes.add(
                  new PermissionAttribute(s, classAnnotations.scope(), requestContext.getMethod())));
    }

    if (methodPermissionAttributes.isEmpty()) {
      return classPermissionAttributes;
    } else {
      return methodPermissionAttributes;
    }
  }

  private void updateUserInAuditRecord(User user) {
    auditService.updateUser(auditHelper.get(), user.getPublicUser());
  }

  private String extractToken(ContainerRequestContext requestContext) {
    String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new WingsException(INVALID_TOKEN);
    }
    return authorizationHeader.substring("Bearer".length()).trim();
  }
}
