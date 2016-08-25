package software.wings.security;

import static javax.ws.rs.Priorities.AUTHENTICATION;
import static software.wings.beans.ErrorCodes.INVALID_TOKEN;
import static software.wings.dl.PageRequest.PageRequestType.LIST_WITHOUT_APP_ID;
import static software.wings.dl.PageRequest.PageRequestType.OTHER;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.common.AuditHelper;
import software.wings.dl.PageRequest.PageRequestType;
import software.wings.exception.WingsException;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.ListAPI;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.AuthService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

/**
 * Created by anubhaw on 3/11/16.
 */
@Singleton
@Priority(AUTHENTICATION)
@AuthRule
public class AuthRuleFilter implements ContainerRequestFilter {
  @Context private ResourceInfo resourceInfo;

  @Inject private AuditService auditService;

  @Inject private AuditHelper auditHelper;

  @Inject private AuthService authService;

  /* (non-Javadoc)
   * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
   */
  @Override
  public void filter(ContainerRequestContext requestContext) {
    String tokenString = extractToken(requestContext);
    AuthToken authToken = authService.validateToken(tokenString);
    User user = authToken.getUser();
    if (user != null) {
      requestContext.setProperty("USER", user);
      updateUserInAuditRecord(user); // FIXME: find better place
      UserThreadLocal.set(user);
    }
    String appId = requestContext.getUriInfo().getQueryParameters().getFirst("appId");
    String envId = requestContext.getUriInfo().getQueryParameters().getFirst("envId");

    setRequestAndResourceType(requestContext, appId);

    List<PermissionAttribute> requiredPermissionAttributes = getAllRequiredPermissionAttributes();
    authService.authorize(appId, envId, user, requiredPermissionAttributes,
        (PageRequestType) requestContext.getProperty("pageRequestType"));
  }

  private void setRequestAndResourceType(ContainerRequestContext requestContext, String appId) {
    if (Strings.isNullOrEmpty(appId)) {
      ListAPI listAPI = resourceInfo.getResourceMethod().getAnnotation(ListAPI.class);
      if (LIST_WITHOUT_APP_ID.equals(listAPI)) {
        requestContext.setProperty("pageRequestType", LIST_WITHOUT_APP_ID);
        requestContext.setProperty("resourceType", listAPI.value());
      } else {
        requestContext.setProperty("pageRequestType", OTHER);
      }
    }
  }

  private List<PermissionAttribute> getAllRequiredPermissionAttributes() {
    List<PermissionAttribute> permissionAttributes = new ArrayList<>();
    List<PermissionAttribute> methodPermissionAttributes = new ArrayList<>();
    List<PermissionAttribute> classPermissionAttributes = new ArrayList<>();

    Method resourceMethod = resourceInfo.getResourceMethod();
    AuthRule methodAnnotations = resourceMethod.getAnnotation(AuthRule.class);
    if (null != methodAnnotations && methodAnnotations.value().length > 0) {
      methodPermissionAttributes.addAll(Lists.newArrayList(methodAnnotations.value()));
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    AuthRule classAnnotations = resourceClass.getAnnotation(AuthRule.class);
    if (null != classAnnotations && classAnnotations.value().length > 0) {
      classPermissionAttributes.addAll(Lists.newArrayList(classAnnotations.value()));
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
