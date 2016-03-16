package software.wings.resources;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

import io.dropwizard.jersey.caching.CacheControl;
import software.wings.app.WingsBootstrap;
import software.wings.audit.AuditHeader;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.service.intfc.AuditService;

@Path("/audits")
public class AuditResource {
  private AuditService httpAuditService;

  public AuditResource() {
    httpAuditService = WingsBootstrap.lookup(AuditService.class);
  }
  public AuditResource(AuditService httpAuditService) {
    this.httpAuditService = httpAuditService;
  }

  @GET
  @Timed
  @ExceptionMetered
  @CacheControl(maxAge = 15, maxAgeUnit = TimeUnit.MINUTES)
  @Produces("application/json")
  public RestResponse<PageResponse<AuditHeader>> list(@BeanParam PageRequest<AuditHeader> pageRequest) {
    return new RestResponse<PageResponse<AuditHeader>>(httpAuditService.list(pageRequest));
  }
}
