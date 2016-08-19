package software.wings.security;

import static javax.ws.rs.Priorities.AUTHENTICATION;
import static software.wings.beans.ErrorCodes.INVALID_TOKEN;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.common.AuditHelper;
import software.wings.dl.GenericDbCache;
import software.wings.exception.WingsException;
import software.wings.security.annotations.AuthRule;
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

  @Inject private GenericDbCache dbCache;

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
    List<PermissionAttr> permissionAttrs = getAccessTypes();
    authService.authorize(requestContext.getUriInfo().getPathParameters().getFirst("appId"),
        requestContext.getUriInfo().getPathParameters().getFirst("envId"), user, permissionAttrs);
  }

  private List<PermissionAttr> getAccessTypes() {
    List<PermissionAttr> permissionAttrs = new ArrayList<>();
    List<PermissionAttr> methodPermissionAttrs = new ArrayList<>();
    List<PermissionAttr> classPermissionAttrs = new ArrayList<>();

    Method resourceMethod = resourceInfo.getResourceMethod();
    AuthRule methodAnnotations = resourceMethod.getAnnotation(AuthRule.class);
    if (null != methodAnnotations && methodAnnotations.value().length > 0) {
      methodPermissionAttrs.addAll(Lists.newArrayList(methodAnnotations.value()));
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    AuthRule classAnnotations = resourceClass.getAnnotation(AuthRule.class);
    if (null != classAnnotations && classAnnotations.value().length > 0) {
      classPermissionAttrs.addAll(Lists.newArrayList(classAnnotations.value()));
    }
    permissionAttrs.addAll(methodPermissionAttrs);
    permissionAttrs.removeAll(classPermissionAttrs);
    permissionAttrs.addAll(classPermissionAttrs);
    return permissionAttrs;
  }

  private void updateUserInAuditRecord(User user) {
    auditService.updateUser(auditHelper.get(), User.getPublicUser(user));
  }

  private String extractToken(ContainerRequestContext requestContext) {
    String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new WingsException(INVALID_TOKEN);
    }
    return authorizationHeader.substring("Bearer".length()).trim();
  }
}
