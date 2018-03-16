package software.wings.filter;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.audit.AuditHeader;
import software.wings.beans.HttpMethod;
import software.wings.common.AuditHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;

/**
 * AuditResponseFilter preserves the http response details.
 *
 * @author Rishi
 */
@Singleton
public class AuditResponseFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(AuditResponseFilter.class);
  private static final List<String> authAwareResources =
      Arrays.asList("/apps", "/services", "/environments", "/workflows", "/pipelines");

  @Inject private AuditHelper auditHelper;
  @Inject private ExecutorService executorService;
  @Inject private AuthService authService;
  @Inject private AppService appService;

  @Context private ContainerRequestContext requestContext;

  /* (non-Javadoc)
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  ;
  @Override
  public void init(FilterConfig arg0) {}

  /* (non-Javadoc)
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   * javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    invalidateAccountCacheIfNeeded(request);
    AuditHeader header = auditHelper.get();
    if (header != null) {
      String path = ((HttpServletRequest) request).getPathInfo();
      if (logger.isDebugEnabled()) {
        logger.debug("path :" + path);
      }
      if (response.getCharacterEncoding() == null) {
        response.setCharacterEncoding("UTF-8");
      }

      HttpServletResponseCopier responseCopier = new HttpServletResponseCopier((HttpServletResponse) response);

      try {
        chain.doFilter(request, responseCopier);
        responseCopier.flushBuffer();
      } finally {
        byte[] copy = responseCopier.getCopy();
        header.setResponseTime(System.currentTimeMillis());
        header.setResponseStatusCode(((HttpServletResponse) response).getStatus());
        auditHelper.finalizeAudit(header, copy);
      }
    } else {
      chain.doFilter(request, response);
    }
  }

  private void invalidateAccountCacheIfNeeded(ServletRequest servletRequest) {
    try {
      HttpServletRequest request = (HttpServletRequest) servletRequest;
      String httpMethod = request.getMethod();
      String resourcePath = request.getPathInfo();

      if (asList(HttpMethod.DELETE.name(), HttpMethod.PUT.name(), HttpMethod.POST.name()).contains(httpMethod)
          && authAwareResources.stream().anyMatch(resourcePath::startsWith)) {
        String accountId = servletRequest.getParameter("accountId");
        String appId = servletRequest.getParameter("appId");

        if (resourcePath.startsWith("/apps") && isEmpty(accountId)
            && isEmpty(appId)) { // Special handling for AppResource
          appId = resourcePath.split("/")[2];
        }

        if (isEmpty(accountId) && isEmpty(appId)) {
          logger.error(
              "Cache eviction failed for resource 2 [{}]", request.getRequestURL() + "?" + request.getQueryString());
          return;
        }

        accountId = isEmpty(accountId) ? appService.getAccountIdByAppId(appId) : accountId;
        String finalAccountId = accountId;
        executorService.submit(() -> authService.evictAccountUserPermissionInfoCache(finalAccountId));
      }

    } catch (Exception ex) {
      logger.error("Cache eviction failed", ex);
    }
  }

  /* (non-Javadoc)
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {}
}
