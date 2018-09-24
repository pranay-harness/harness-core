package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.service.impl.bugsnag.BugsnagApplication;
import software.wings.service.intfc.analysis.LogVerificationService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * @author Praveen
 */

@Api("log-verification")
@Path("/log-verification")
@Produces("application/json")
public class BugsnagResource {
  @Inject private LogVerificationService apmVerificationService;

  @GET
  @Path("/bugsnag-applications")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<BugsnagApplication>> getBugsnagApplications(@QueryParam("accountId") String accountId,
      @QueryParam("settingId") final String settingId, @QueryParam("organizationId") final String orgId)
      throws IOException {
    return new RestResponse<>(
        apmVerificationService.getOrgProjectListBugsnag(settingId, orgId, StateType.BUG_SNAG, true));
  }

  @GET
  @Path("/bugsnag-orgs")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<BugsnagApplication>> getBugsnagOrganizations(
      @QueryParam("accountId") String accountId, @QueryParam("settingId") final String settingId) throws IOException {
    return new RestResponse<>(
        apmVerificationService.getOrgProjectListBugsnag(settingId, "", StateType.BUG_SNAG, false));
  }
}
