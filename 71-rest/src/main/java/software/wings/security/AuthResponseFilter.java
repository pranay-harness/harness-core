package software.wings.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.manage.GlobalContextManager;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.ContainerRequest;
import software.wings.beans.HttpMethod;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;

import java.util.Set;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by anubhaw on 4/20/16.
 */
@Singleton
@Slf4j
public class AuthResponseFilter implements ContainerResponseFilter {
  private static final String RESOURCE_URI_CREATE_APP = "/api/apps";
  private static final String RESOURCE_URI_CREATE_SERVICE = "/api/services";
  private static final String RESOURCE_URI_CREATE_PROVISIONER = "/api/infrastructure-provisioners";
  private static final String RESOURCE_URI_CREATE_ENVIRONMENT = "/api/environments";
  private static final String RESOURCE_URI_CREATE_WORKFLOW = "/api/workflows";
  private static final String RESOURCE_URI_CREATE_PIPELINE = "/api/pipelines";
  private static final String RESOURCE_URI_CREATE_DASHBOARD = "/api/custom-dashboard";

  private static final String RESOURCE_URI_CLONE_APP = "/api/apps/[^/]+/clone";
  private static final String RESOURCE_URI_CLONE_SERVICE = "/api/services/[^/]+/clone";
  private static final String RESOURCE_URI_CLONE_PROVISIONER = "/api/infrastructure-provisioners/[^/]+/clone";
  private static final String RESOURCE_URI_CLONE_ENVIRONMENT = "/api/environments/[^/]+/clone";
  private static final String RESOURCE_URI_CLONE_WORKFLOW = "/api/workflows/[^/]+/clone";
  private static final String RESOURCE_URI_CLONE_PIPELINE = "/api/pipelines/[^/]+/clone";

  private static final String RESOURCE_URI_UPDATE_ENVIRONMENT = "/api/environments/[^/]+";
  private static final String RESOURCE_URI_UPDATE_WORKFLOW = "/api/workflows/[^/]+/basic";
  private static final String RESOURCE_URI_UPDATE_PIPELINE = "/api/pipelines/[^/]+";
  private static final String RESOURCE_URI_UPDATE_DASHBOARD = "/api/custom-dashboard";

  private static final String RESOURCE_URI_DELETE_APP = "/api/apps/[^/]+";
  private static final String RESOURCE_URI_DELETE_ENVIRONMENT = "/api/environments/[^/]+";
  private static final String RESOURCE_URI_DELETE_DASHBOARD = "/api/custom-dashboard";

  private static final Set<String> restResourcesCreateURIs = Sets.newHashSet(RESOURCE_URI_CREATE_APP,
      RESOURCE_URI_CREATE_SERVICE, RESOURCE_URI_CREATE_PROVISIONER, RESOURCE_URI_CREATE_ENVIRONMENT,
      RESOURCE_URI_CREATE_WORKFLOW, RESOURCE_URI_CREATE_PIPELINE, RESOURCE_URI_CREATE_DASHBOARD);
  private static final Set<String> restResourcesCloneURIs =
      Sets.newHashSet(RESOURCE_URI_CLONE_APP, RESOURCE_URI_CLONE_SERVICE, RESOURCE_URI_CLONE_PROVISIONER,
          RESOURCE_URI_CLONE_ENVIRONMENT, RESOURCE_URI_CLONE_WORKFLOW, RESOURCE_URI_CLONE_PIPELINE);
  private static final Set<String> restResourcesUpdateURIs = Sets.newHashSet(RESOURCE_URI_UPDATE_ENVIRONMENT,
      RESOURCE_URI_UPDATE_WORKFLOW, RESOURCE_URI_UPDATE_PIPELINE, RESOURCE_URI_UPDATE_DASHBOARD);

  @Inject private AuthService authService;
  @Inject private AppService appService;

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    GlobalContextManager.unset();
    UserThreadLocal.unset(); // clear user object from thread local
    HarnessUserThreadLocal.unset(); // clean the isHarnessUser flag from thread local
    invalidateAccountCacheIfNeeded(requestContext);
  }

  private void invalidateAccountCacheIfNeeded(ContainerRequestContext requestContext) {
    String httpMethod = requestContext.getMethod();
    String resourcePath = requestContext.getUriInfo().getAbsolutePath().getPath();

    if (HttpMethod.PUT.name().equals(httpMethod) && restResourcesUpdateURIs.stream().anyMatch(resourcePath::matches)) {
      if (resourcePath.matches(RESOURCE_URI_UPDATE_ENVIRONMENT)) {
        evictPermissionsAndRestrictions(requestContext, resourcePath, true, true);
      } else {
        evictPermissions(requestContext, resourcePath, true);
      }
    } else if (HttpMethod.POST.name().equals(httpMethod)
        && (restResourcesCreateURIs.contains(resourcePath)
               || restResourcesCloneURIs.stream().anyMatch(resourcePath::matches))) {
      if (resourcePath.equals(RESOURCE_URI_CREATE_APP) || resourcePath.equals(RESOURCE_URI_CREATE_ENVIRONMENT)
          || resourcePath.matches(RESOURCE_URI_CLONE_ENVIRONMENT)) {
        evictPermissionsAndRestrictions(requestContext, resourcePath, true, true);
      } else {
        evictPermissions(requestContext, resourcePath, true);
      }
    } else if (HttpMethod.DELETE.name().equals(httpMethod)) {
      if (resourcePath.matches(RESOURCE_URI_DELETE_APP) || resourcePath.matches(RESOURCE_URI_DELETE_ENVIRONMENT)) {
        evictPermissionsAndRestrictions(requestContext, resourcePath, true, true);
      } else if (resourcePath.matches(RESOURCE_URI_DELETE_DASHBOARD)) {
        evictPermissions(requestContext, resourcePath, true);
      }
    }
  }

  private void evictPermissions(
      ContainerRequestContext requestContext, String resourcePath, boolean rebuildUserPermissionInfo) {
    MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();
    String accountId = queryParameters.getFirst("accountId");
    try {
      String appId = queryParameters.getFirst("appId");

      // Special handling for AppResource
      if (resourcePath.startsWith(RESOURCE_URI_CREATE_APP) && isEmpty(accountId) && isEmpty(appId)) {
        appId = requestContext.getUriInfo().getPathParameters().getFirst("appId");
      }

      if (isEmpty(accountId) && isEmpty(appId)) {
        logger.error("Cache eviction failed for resource 2 [{}]", ((ContainerRequest) requestContext).getRequestUri());
        return;
      }

      accountId = isEmpty(accountId) ? appService.getAccountIdByAppId(appId) : accountId;

      authService.evictUserPermissionCacheForAccount(accountId, rebuildUserPermissionInfo);
    } catch (Exception ex) {
      logger.error("Cache eviction failed for resourcePath {} for accountId {}", resourcePath, accountId, ex);
    }
  }

  private void evictPermissionsAndRestrictions(ContainerRequestContext requestContext, String resourcePath,
      boolean rebuildUserPermissionInfo, boolean rebuildUserRestrictionInfo) {
    MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();
    String accountId = queryParameters.getFirst("accountId");
    try {
      String appId = queryParameters.getFirst("appId");

      // Special handling for AppResource
      if (resourcePath.startsWith(RESOURCE_URI_CREATE_APP) && isEmpty(accountId) && isEmpty(appId)) {
        appId = requestContext.getUriInfo().getPathParameters().getFirst("appId");
      }

      if (isEmpty(accountId) && isEmpty(appId)) {
        logger.error("Cache eviction failed for resource 2 [{}]", ((ContainerRequest) requestContext).getRequestUri());
        return;
      }

      accountId = isEmpty(accountId) ? appService.getAccountIdByAppId(appId) : accountId;

      authService.evictUserPermissionAndRestrictionCacheForAccount(
          accountId, rebuildUserPermissionInfo, rebuildUserRestrictionInfo);
    } catch (Exception ex) {
      logger.error("Cache eviction failed for resourcePath {} for accountId {}", resourcePath, accountId, ex);
    }
  }
}
