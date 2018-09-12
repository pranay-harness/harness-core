package software.wings.filter;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.network.Localhost.getLocalHostAddress;
import static io.harness.network.Localhost.getLocalHostName;
import static java.util.Arrays.asList;
import static software.wings.common.Constants.FILE_CONTENT_NOT_STORED;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.exception.WingsException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.beans.HttpMethod;
import software.wings.common.AuditHelper;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.utils.BoundedInputStream;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

/**
 * AuditRequestFilter preserves the rest endpoint header and payload.
 *
 * @author Rishi
 */
@Singleton
@Provider
@Priority(1500) // Authorization > Audit > Authentication
public class AuditRequestFilter implements ContainerRequestFilter {
  private static final Logger logger = LoggerFactory.getLogger(AuditRequestFilter.class);
  @Context private ResourceContext resourceContext;
  @Context private ResourceInfo resourceInfo;

  @Inject private AuditHelper auditHelper;
  @Inject private FileService fileService;
  @Inject private MainConfiguration configuration;

  /* (non-Javadoc)
   * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
   */
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    if (isAuditExemptedHttpMethod(requestContext) || isAuditExemptedResource()) {
      // do not audit idempotent HttpMethod until we have finer control auditing.
      return;
    }

    AuditHeader header = new AuditHeader();
    String url = requestContext.getUriInfo().getAbsolutePath().toString();
    header.setUrl(url);

    String headerString = getHeaderString(requestContext.getHeaders());
    header.setHeaderString(headerString);

    String query = getQueryParams(requestContext.getUriInfo().getQueryParameters());
    header.setQueryParams(query);

    HttpMethod method = HttpMethod.valueOf(requestContext.getMethod());
    header.setRequestMethod(method);
    header.setResourcePath(requestContext.getUriInfo().getPath());
    header.setRequestTime(System.currentTimeMillis());

    HttpServletRequest request = resourceContext.getResource(HttpServletRequest.class);

    header.setRemoteHostName(request.getRemoteHost());
    header.setRemoteIpAddress(request.getRemoteAddr());
    header.setRemoteHostPort(request.getRemotePort());
    header.setLocalHostName(getLocalHostName());
    header.setLocalIpAddress(getLocalHostAddress());

    header = auditHelper.create(header);

    try {
      if (headerString.contains("multipart/form-data")) {
        // don't store file content in audit logs
        auditHelper.create(
            header, RequestType.REQUEST, IOUtils.toInputStream(FILE_CONTENT_NOT_STORED, Charset.defaultCharset()));
      } else {
        try (BoundedInputStream inputStream = new BoundedInputStream(
                 requestContext.getEntityStream(), configuration.getFileUploadLimits().getAppContainerLimit())) {
          String fileId = auditHelper.create(header, RequestType.REQUEST, inputStream);
          requestContext.setEntityStream(fileService.openDownloadStream(fileId, FileBucket.AUDITS));
        }
      }
    } catch (Exception exception) {
      throw new WingsException(exception);
    }
  }

  private boolean isAuditExemptedResource() {
    return resourceInfo.getResourceMethod().getAnnotation(DelegateAuth.class) != null
        || resourceInfo.getResourceMethod().getAnnotation(LearningEngineAuth.class) != null;
  }

  private boolean isAuditExemptedHttpMethod(ContainerRequestContext requestContext) {
    return asList(HttpMethod.GET.name(), HttpMethod.OPTIONS.name(), HttpMethod.HEAD.name())
        .contains(requestContext.getMethod());
  }

  @SuppressFBWarnings("WMI_WRONG_MAP_ITERATOR")
  private String getHeaderString(MultivaluedMap<String, String> headers) {
    if (isEmpty(headers)) {
      return "";
    }

    StringBuilder headerString = new StringBuilder();
    for (String key : headers.keySet()) {
      headerString.append(key).append('=');
      for (String value : headers.get(key)) {
        headerString.append(';');
        headerString.append(key.equalsIgnoreCase("Authorization") ? "********" : value);
      }
      headerString.substring(1);
      headerString.append(',');
    }
    String headerStr = headerString.toString();
    if (headerStr.length() > 0) {
      headerStr = headerStr.substring(0, headerStr.length() - 1);
    }
    return headerStr;
  }

  @SuppressFBWarnings({"SBSC_USE_STRINGBUFFER_CONCATENATION"})
  private String getQueryParams(MultivaluedMap<String, String> queryParameters) {
    String queryParams = "";
    for (Entry<String, List<String>> entry : queryParameters.entrySet()) {
      String key = entry.getKey();
      String temp = "";
      for (String value : queryParameters.get(key)) {
        temp += "&" + key + "=" + value;
      }
      queryParams += "&" + temp.substring(1);
    }
    if (queryParams.equals("")) {
      return null;
    } else {
      return queryParams.substring(1);
    }
  }
}
