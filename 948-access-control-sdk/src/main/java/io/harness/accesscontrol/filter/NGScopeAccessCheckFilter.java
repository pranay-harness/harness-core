/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.filter;

import static io.harness.accesscontrol.filter.ScopePermissions.VIEW_ACCOUNT_PERMISSION;
import static io.harness.accesscontrol.filter.ScopePermissions.VIEW_ORGANIZATION_PERMISSION;
import static io.harness.accesscontrol.filter.ScopePermissions.VIEW_PROJECT_PERMISSION;
import static io.harness.accesscontrol.filter.ScopeResourceTypes.ACCOUNT;
import static io.harness.accesscontrol.filter.ScopeResourceTypes.ORGANIZATION;
import static io.harness.accesscontrol.filter.ScopeResourceTypes.PROJECT;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.PublicApi;
import io.harness.utils.ScopeUtils;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Predicate;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(PL)
@Slf4j
public class NGScopeAccessCheckFilter implements ContainerRequestFilter {
  public static final String X_REQUEST_FROM_GATEWAY = "X-Request-From-Gateway";
  Predicate<Pair<ResourceInfo, ContainerRequestContext>> bypassFilter;
  AccessControlClient accessControlClient;
  @Context private ResourceInfo resourceInfo;

  public NGScopeAccessCheckFilter(List<Predicate<Pair<ResourceInfo, ContainerRequestContext>>> bypassFilters,
      AccessControlClient accessControlClient) {
    if (isEmpty(bypassFilters)) {
      throw new AssertionError("BypassFilters can't be empty");
    }
    this.bypassFilter = bypassFilters.get(0);
    bypassFilters.stream().skip(1).forEach(filter -> this.bypassFilter = this.bypassFilter.or(filter));
    this.accessControlClient = accessControlClient;
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext) {
    if (!bypassFilter.test(Pair.of(resourceInfo, containerRequestContext))) {
      MultivaluedMap<String, String> queryParameters = containerRequestContext.getUriInfo().getQueryParameters();
      String accountIdentifier = queryParameters.getFirst(NGCommonEntityConstants.ACCOUNT_KEY);
      String orgIdentifier = queryParameters.getFirst(NGCommonEntityConstants.ORG_KEY);
      String projectIdentifier = queryParameters.getFirst(NGCommonEntityConstants.PROJECT_KEY);

      if (isBlank(accountIdentifier) && isBlank(orgIdentifier) && isBlank(projectIdentifier)) {
        log.warn(
            "{} corresponding to endpoint {} doesn't conform to conventions. It should take query params accountIdentifier, orgIdentifier, projectIdentifier as applicable",
            resourceInfo.getResourceMethod(), containerRequestContext.getUriInfo().getPath());
        return;
      }

      if (isBlank(accountIdentifier) || (isBlank(orgIdentifier) && isNoneBlank(projectIdentifier))) {
        log.warn(
            "Set of scope params are invalid for path {}. [accountIdentifier: {}, orgIdentifier: {}, projectIdentifier: {}]",
            containerRequestContext.getUriInfo().getPath(), accountIdentifier, orgIdentifier, projectIdentifier);
        return;
      }

      Scope scope = Scope.of(accountIdentifier, orgIdentifier, projectIdentifier);
      if (!checkAccess(scope)) {
        log.warn("{} doesn't have permission to access this scope {} while making request {}",
            SourcePrincipalContextBuilder.getSourcePrincipal().getName(), ScopeUtils.toString(scope),
            containerRequestContext.getUriInfo().getPath());
      }
    }
  }

  public boolean checkAccess(Scope scope) {
    if (isNoneBlank(scope.getProjectIdentifier())) {
      return accessControlClient.hasAccess(ResourceScope.builder()
                                               .accountIdentifier(scope.getAccountIdentifier())
                                               .orgIdentifier(scope.getOrgIdentifier())
                                               .build(),
          Resource.of(PROJECT, scope.getProjectIdentifier()), VIEW_PROJECT_PERMISSION);
    } else if (isNoneBlank(scope.getOrgIdentifier())) {
      return accessControlClient.hasAccess(
          ResourceScope.builder().accountIdentifier(scope.getAccountIdentifier()).build(),
          Resource.of(ORGANIZATION, scope.getOrgIdentifier()), VIEW_ORGANIZATION_PERMISSION);
    } else {
      return accessControlClient.hasAccess(
          ResourceScope.builder().build(), Resource.of(ACCOUNT, scope.getAccountIdentifier()), VIEW_ACCOUNT_PERMISSION);
    }
  }

  public static Predicate<Pair<ResourceInfo, ContainerRequestContext>> bypassPublicApi() {
    return getAnnotationFilterPredicate(PublicApi.class);
  }

  public static Predicate<Pair<ResourceInfo, ContainerRequestContext>> bypassInternalApi() {
    return getAnnotationFilterPredicate(InternalApi.class);
  }

  public static Predicate<Pair<ResourceInfo, ContainerRequestContext>> bypassPaths(List<String> paths) {
    return resourceInfoAndRequest
        -> paths.stream().anyMatch(
            path -> resourceInfoAndRequest.getValue().getUriInfo().getAbsolutePath().getPath().endsWith(path));
  }

  private static Predicate<Pair<ResourceInfo, ContainerRequestContext>> getAnnotationFilterPredicate(
      Class<? extends Annotation> annotation) {
    return resourceInfoAndRequest
        -> (resourceInfoAndRequest.getKey().getResourceMethod() != null
               && resourceInfoAndRequest.getKey().getResourceMethod().getAnnotation(annotation) != null)
        || (resourceInfoAndRequest.getKey().getResourceClass() != null
            && resourceInfoAndRequest.getKey().getResourceClass().getAnnotation(annotation) != null);
  }

  public static Predicate<Pair<ResourceInfo, ContainerRequestContext>> bypassInterMsvcRequests() {
    return resourceInfoAndRequest
        -> !TRUE.toString().equals(resourceInfoAndRequest.getValue().getHeaders().getFirst(X_REQUEST_FROM_GATEWAY));
  }
}
