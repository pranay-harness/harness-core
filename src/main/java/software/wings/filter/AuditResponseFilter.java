package software.wings.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.audit.AuditHeader;
import software.wings.common.AuditHelper;

import java.io.IOException;
import java.sql.Timestamp;
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

/**
 * AuditResponseFilter preserves the http response details.
 *
 * @author Rishi
 */
@Singleton
public class AuditResponseFilter implements Filter {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private AuditHelper auditHelper;

  @Override
  public void init(FilterConfig arg0) throws ServletException {}

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
        header.setResponseTime(new Timestamp(System.currentTimeMillis()));
        header.setResponseStatusCode(((HttpServletResponse) response).getStatus());
        auditHelper.finalizeAudit(header, copy);
      }
    }
  }

  @Override
  public void destroy() {}
}
