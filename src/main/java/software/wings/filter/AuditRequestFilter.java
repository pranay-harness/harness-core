package software.wings.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.wings.beans.HTTPMethod;
import software.wings.beans.AuditHeader;
import software.wings.beans.AuditPayload;
import software.wings.beans.AuditPayload.RequestType;
import software.wings.common.AuditHelper;
import software.wings.exception.WingsException;
import software.wings.utils.Misc;

/**
 *  AuditRequestFilter preserves the rest endpoint header and payload.
 *
 *
 * @author Rishi
 *
 */
@Provider
@Priority(1)
public class AuditRequestFilter implements ContainerRequestFilter {
  @Context private ResourceContext resourceContext;

  private AuditHelper auditHelper = AuditHelper.getInstance();

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    AuditHeader header = new AuditHeader();
    String url = requestContext.getUriInfo().getAbsolutePath().toString();
    header.setUrl(url);

    String headerString = getHeaderString(requestContext.getHeaders());
    header.setHeaderString(headerString);

    String query = getQueryParams(requestContext.getUriInfo().getQueryParameters());
    header.setQueryParams(query);

    HTTPMethod method = HTTPMethod.valueOf(requestContext.getMethod());
    header.setRequestMethod(method);
    header.setResourcePath(requestContext.getUriInfo().getPath());
    header.setRequestTime(new Timestamp(System.currentTimeMillis()));

    HttpServletRequest request = resourceContext.getResource(HttpServletRequest.class);
    if (request.getRemoteUser() != null) {
      // TODO
      // header.setRemoteUser(new User(request.getRemoteUser()));
    }
    header.setRemoteHostName(request.getRemoteHost());
    header.setRemoteIpAddress(request.getRemoteAddr());
    header.setRemoteHostPort(request.getRemotePort());
    header.setLocalHostName(InetAddress.getLocalHost().getHostName());
    header.setLocalIpAddress(InetAddress.getLocalHost().getHostAddress());

    header = auditHelper.create(header);

    InputStream entityStream = null;
    ByteArrayInputStream byteArrayInputStream = null;
    try {
      AuditPayload detail = new AuditPayload();
      entityStream = requestContext.getEntityStream();

      byte[] httpBody = IOUtils.toByteArray(entityStream);

      if (httpBody != null) {
        byteArrayInputStream = new ByteArrayInputStream(httpBody);

        requestContext.setEntityStream(byteArrayInputStream);
        detail.setHeaderId(header.getUuid());
        detail.setRequestType(RequestType.REQUEST);
        detail.setPayload(httpBody);
        auditHelper.create(detail);
      }

    } catch (Exception exception) {
      throw new WingsException(exception);
    } finally {
      Misc.quietClose(entityStream, byteArrayInputStream);
    }
  }

  private String getQueryParams(MultivaluedMap<String, String> queryParameters) {
    String queryParams = "";
    for (String key : queryParameters.keySet()) {
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

  private String getHeaderString(MultivaluedMap<String, String> headers) {
    if (headers == null || headers.isEmpty()) {
      return "";
    } else {
      StringBuilder headerString = new StringBuilder();
      for (String key : headers.keySet()) {
        headerString.append(key).append("=");
        for (String value : headers.get(key)) {
          headerString.append(";");
          headerString.append(value);
        }
        headerString.substring(1);
        headerString.append(",");
      }
      String headerStr = headerString.toString();
      if (headerStr.length() > 0) {
        headerStr = headerStr.substring(0, headerStr.length() - 1);
      }
      return headerStr;
    }
  }

  private static Logger logger = LoggerFactory.getLogger(AuditRequestFilter.class);
}