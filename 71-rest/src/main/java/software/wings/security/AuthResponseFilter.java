package software.wings.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Arrays.asList;
import static software.wings.beans.HttpMethod.POST;
import static software.wings.beans.HttpMethod.PUT;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.glassfish.jersey.server.ContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;

import java.util.List;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by anubhaw on 4/20/16.
 */
@Singleton
public class AuthResponseFilter implements ContainerResponseFilter {
  private static final Logger logger = LoggerFactory.getLogger(AuthResponseFilter.class);
  private static final List<String> authAwareResources = asList("/api/apps", "/api/services",
      "/api/infrastructure-provisioners", "/api/environments", "/api/workflows", "/api/pipelines");

  @Inject private AuthService authService;
  @Inject private AppService appService;

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    UserThreadLocal.unset(); // clear user object from thread local
    invalidateAccountCacheIfNeeded(requestContext);
  }

  private void invalidateAccountCacheIfNeeded(ContainerRequestContext requestContext) {
    try {
      String httpMethod = requestContext.getMethod();
      String resourcePath = requestContext.getUriInfo().getAbsolutePath().getPath();

      if (asList(PUT.name(), POST.name()).contains(httpMethod)
          && authAwareResources.stream().anyMatch(resourcePath::startsWith)) {
        MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();
        String accountId = queryParameters.getFirst("accountId");
        String appId = queryParameters.getFirst("appId");

        // Special handling for AppResource
        if (resourcePath.startsWith("/api/apps") && isEmpty(accountId) && isEmpty(appId)) {
          appId = requestContext.getUriInfo().getPathParameters().getFirst("appId");
        }

        if (isEmpty(accountId) && isEmpty(appId)) {
          logger.error(
              "Cache eviction failed for resource 2 [{}]", ((ContainerRequest) requestContext).getRequestUri());
          return;
        }

        accountId = isEmpty(accountId) ? appService.getAccountIdByAppId(appId) : accountId;
        String finalAccountId = accountId;
        authService.evictAccountUserPermissionInfoCache(finalAccountId, false);
      }
    } catch (Exception ex) {
      logger.error("Cache eviction failed", ex);
    }
  }
}
