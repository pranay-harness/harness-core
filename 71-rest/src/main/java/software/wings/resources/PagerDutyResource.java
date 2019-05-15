package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.service.intfc.pagerduty.PagerDutyService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/pagerduty")
@Produces("application/json")
public class PagerDutyResource {
  @Inject PagerDutyService pagerDutyService;

  @GET
  @Path("validate-key")
  @Timed
  @ExceptionMetered
  public RestResponse validatePagerDutyIntegrationKey(@QueryParam("appId") String appId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("pagerDutyKey") String pagerDutyKey) {
    return new RestResponse<>(pagerDutyService.validateKey(pagerDutyKey));
  }
}
