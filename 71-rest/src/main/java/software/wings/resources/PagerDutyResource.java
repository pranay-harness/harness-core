package software.wings.resources;

import io.harness.rest.RestResponse;

import software.wings.service.intfc.pagerduty.PagerDutyService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;

@Path("/pagerduty")
@Produces("application/json")
public class PagerDutyResource {
  @Inject PagerDutyService pagerDutyService;

  @GET
  @Path("validate-key")
  @Timed
  @ExceptionMetered
  public RestResponse validatePagerDutyIntegrationKey(
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("pagerDutyKey") String pagerDutyKey) {
    return new RestResponse<>(pagerDutyService.validateKey(pagerDutyKey));
  }

  @GET
  @Path("create-test-incident")
  @Timed
  @ExceptionMetered
  public RestResponse createTestIncident(
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("pagerDutyKey") String pagerDutyKey) {
    return new RestResponse<>(pagerDutyService.validateCreateTestEvent(pagerDutyKey));
  }
}
