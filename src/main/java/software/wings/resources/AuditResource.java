package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import software.wings.audit.AuditHeader;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.AuditService;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Api("audits")
@Path("/audits")
public class AuditResource {
  private AuditService httpAuditService;

  @Inject
  public AuditService getHttpAuditService() {
    return httpAuditService;
  }

  public void setHttpAuditService(AuditService httpAuditService) {
    this.httpAuditService = httpAuditService;
  }

  @GET
  @Timed
  @ExceptionMetered
  @CacheControl(maxAge = 15, maxAgeUnit = TimeUnit.MINUTES)
  @Produces("application/json")
  public RestResponse<PageResponse<AuditHeader>> list(@BeanParam PageRequest<AuditHeader> pageRequest) {
    return new RestResponse<>(httpAuditService.list(pageRequest));
  }
}
