package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.security.annotations.DelegateAuth;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.bugsnag.BugsnagApplication;
import software.wings.service.impl.bugsnag.BugsnagSetupTestData;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.analysis.LogVerificationService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.Set;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * @author Praveen
 */

@Api("bugsnag")
@Path("/bugsnag")
@Produces("application/json")
@Slf4j
public class BugsnagResource {
  @Inject private LogVerificationService logVerificationService;

  @GET
  @Path("/applications")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<BugsnagApplication>> getBugsnagApplications(@QueryParam("accountId") String accountId,
      @QueryParam("settingId") final String settingId, @QueryParam("organizationId") final String orgId)
      throws IOException {
    logger.info("Fetching bugsnag Applications for accountId {}, orgId {}", accountId, orgId);
    return new RestResponse<>(
        logVerificationService.getOrgProjectListBugsnag(settingId, orgId, StateType.BUG_SNAG, true));
  }

  @GET
  @Path("/orgs")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<BugsnagApplication>> getBugsnagOrganizations(
      @QueryParam("accountId") String accountId, @QueryParam("settingId") final String settingId) throws IOException {
    logger.info("Fetching bugsnag Organizations for accountId {}", accountId);
    return new RestResponse<>(
        logVerificationService.getOrgProjectListBugsnag(settingId, "", StateType.BUG_SNAG, false));
  }

  /**
   * API to get log Records based on provided node data.
   *
   * @param accountId : account id.
   * @param bugsnagSetupTestData : configuration details for test node.
   * @return {@link VerificationNodeDataSetupResponse}
   */
  @POST
  @Path(LogAnalysisResource.TEST_NODE_DATA)
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getLogRecords(
      @QueryParam("accountId") String accountId, @Valid BugsnagSetupTestData bugsnagSetupTestData) {
    logger.info("Fetching log Records for verification for accountId : " + accountId
        + " and BugsnagSetupTestData :" + bugsnagSetupTestData);
    return new RestResponse<>(logVerificationService.getTestLogData(accountId, bugsnagSetupTestData));
  }
}
