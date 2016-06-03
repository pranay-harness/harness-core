package software.wings.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.audit.AuditHeader;
import software.wings.common.AuditHelper;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// TODO: Auto-generated Javadoc

/**
 * AuditResponseFilter preserves the http response details.
 *
 * @author Rishi
 */
@Singleton
public class AuditResponseFilter implements Filter {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private AuditHelper auditHelper;

  /* (non-Javadoc)
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  @Override
  public void init(FilterConfig arg0) throws ServletException {}

  /* (non-Javadoc)
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   * javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = ((HttpServletRequest) request).getPathInfo();
    logger.debug("path :" + path);
    if (response.getCharacterEncoding() == null) {
      response.setCharacterEncoding("UTF-8");
    }

    HttpServletResponseCopier responseCopier = new HttpServletResponseCopier((HttpServletResponse) response);

    try {
      chain.doFilter(request, responseCopier);
      responseCopier.flushBuffer();
    } finally {
      byte[] copy = responseCopier.getCopy();

      AuditHeader header = auditHelper.get();
      if (header != null) {
        header.setResponseTime(System.currentTimeMillis());
        header.setResponseStatusCode(((HttpServletResponse) response).getStatus());
        auditHelper.finalizeAudit(header, copy);
      }
    }
  }

  /* (non-Javadoc)
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {}
}
