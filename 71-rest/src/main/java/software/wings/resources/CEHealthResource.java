package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.ccm.health.CEHealthStatus;
import io.harness.ccm.health.HealthStatusService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("cost")
@Path("/cost")
@Produces("application/json")
public class CEHealthResource {
  private HealthStatusService healthStatusService;

  @Inject
  CEHealthResource(HealthStatusService healthStatusService) {
    this.healthStatusService = healthStatusService;
  }

  @GET
  @Path("/health")
  @Timed
  @ExceptionMetered
  public RestResponse<CEHealthStatus> get(
      @QueryParam("accountId") String accountId, @QueryParam("cloudProviderId") String cloudProviderId) {
    return new RestResponse<>(healthStatusService.getHealthStatus(cloudProviderId));
  }
}
