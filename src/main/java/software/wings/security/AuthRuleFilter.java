package software.wings.security;

import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static software.wings.beans.ErrorCodes.INVALID_TOKEN;
import static software.wings.dl.PageRequest.PageRequestType.LIST_WITHOUT_APP_ID;
import static software.wings.dl.PageRequest.PageRequestType.LIST_WITHOUT_ENV_ID;
import static software.wings.dl.PageRequest.PageRequestType.OTHER;

import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.common.AuditHelper;
import software.wings.dl.PageRequest.PageRequestType;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.ListAPI;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
  @Context private ResourceInfo resourceInfo;

  private AuditService auditService;
  private AuditHelper auditHelper;
  private AuthService authService;
  private EnvironmentService environmentService;

  @Inject
  public AuthRuleFilter(AuditService auditService, AuditHelper auditHelper, AuthService authService,
      EnvironmentService environmentService) {
    this.auditService = auditService;
    this.auditHelper = auditHelper;
    this.authService = authService;
    this.environmentService = environmentService;
  }

  /* (non-Javadoc)
   * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
   */
  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (authorizationExemptedRequest(requestContext)) {
      return; // do nothing
    }
    String tokenString = extractToken(requestContext);
    AuthToken authToken = authService.validateToken(tokenString);
    User user = authToken.getUser();
    if (user != null) {
      requestContext.setProperty("USER", user);
      updateUserInAuditRecord(user); // FIXME: find better place
      UserThreadLocal.set(user);
    }

    MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
    MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();

    String appId = getRequestParamFromContext("appId", pathParameters, queryParameters);
    String envId = getRequestParamFromContext("envId", pathParameters, queryParameters);

    setRequestAndResourceType(requestContext, appId);

    List<PermissionAttribute> requiredPermissionAttributes = getAllRequiredPermissionAttributes();
    authService.authorize(appId, envId, user, requiredPermissionAttributes,
        (PageRequestType) requestContext.getProperty("pageRequestType"));
  }

  private boolean authorizationExemptedRequest(ContainerRequestContext requestContext) {
    return publicAPI() || requestContext.getMethod().equals(OPTIONS)
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/version")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith(".well-known");
  }

  private String getRequestParamFromContext(
      String key, MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
    return queryParameters.getFirst(key) != null ? queryParameters.getFirst(key) : pathParameters.getFirst(key);
  }

  private boolean publicAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(AuthRule.class) == null && resourceClass.getAnnotation(AuthRule.class) == null
        && (resourceMethod.getAnnotation(PublicApi.class) != null
               || resourceClass.getAnnotation(PublicApi.class) != null);
  }

  private void setRequestAndResourceType(ContainerRequestContext requestContext, String appId) {
    ListAPI listAPI = resourceInfo.getResourceMethod().getAnnotation(ListAPI.class);
    if (listAPI != null) {
      requestContext.setProperty("resourceType", listAPI.value());
      // TODO:: Improve this logic
      if (listAPI.value() == ResourceType.APPLICATION) {
        requestContext.setProperty("pageRequestType", LIST_WITHOUT_APP_ID);
      } else if (listAPI.value() == ResourceType.ENVIRONMENT) {
        requestContext.setProperty("pageRequestType", LIST_WITHOUT_ENV_ID);
        requestContext.setProperty("appEnvironments", environmentService.getEnvByApp(appId));
      }
    } else {
      requestContext.setProperty("pageRequestType", OTHER);
    }
  }

  private List<PermissionAttribute> getAllRequiredPermissionAttributes() {
    List<PermissionAttribute> permissionAttributes = new ArrayList<>();
    List<PermissionAttribute> methodPermissionAttributes = new ArrayList<>();
    List<PermissionAttribute> classPermissionAttributes = new ArrayList<>();

    Method resourceMethod = resourceInfo.getResourceMethod();
    AuthRule methodAnnotations = resourceMethod.getAnnotation(AuthRule.class);
    if (null != methodAnnotations) {
      Stream.of(methodAnnotations.value())
          .forEach(s -> methodPermissionAttributes.add(new PermissionAttribute(s, methodAnnotations.scope())));
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    AuthRule classAnnotations = resourceClass.getAnnotation(AuthRule.class);
    if (null != classAnnotations) {
      Stream.of(classAnnotations.value())
          .forEach(s -> classPermissionAttributes.add(new PermissionAttribute(s, methodAnnotations.scope())));
    }

    permissionAttributes.addAll(classPermissionAttributes);
    permissionAttributes.removeAll(methodPermissionAttributes);
    permissionAttributes.addAll(methodPermissionAttributes);
    return permissionAttributes;
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
