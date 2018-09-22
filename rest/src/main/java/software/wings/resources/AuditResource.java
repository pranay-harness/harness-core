package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.dropwizard.jersey.caching.CacheControl;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.swagger.annotations.Api;
import software.wings.audit.AuditHeader;
import software.wings.beans.RestResponse;
import software.wings.service.intfc.AuditService;

import java.util.concurrent.TimeUnit;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * The Class AuditResource.
 */
@Api("audits")
@Path("/audits")
public class AuditResource {
  private AuditService httpAuditService;

  /**
   * Gets http audit service.
   *
   * @return the http audit service
   */
  @Inject
  public AuditService getHttpAuditService() {
    return httpAuditService;
  }

  /**
   * Sets http audit service.
   *
   * @param httpAuditService the http audit service
   */
  public void setHttpAuditService(AuditService httpAuditService) {
    this.httpAuditService = httpAuditService;
  }

  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  @CacheControl(maxAge = 15, maxAgeUnit = TimeUnit.MINUTES)
  @Produces("application/json")
  public RestResponse<PageResponse<AuditHeader>> list(@BeanParam PageRequest<AuditHeader> pageRequest) {
    return new RestResponse<>(httpAuditService.list(pageRequest));
  }
}
