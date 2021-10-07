package io.harness.pms.Dashboard;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.OrgProjectIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.dashboards.PipelinesCount;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Api("landingDashboards")
@Path("landingDashboards")
@Produces({"application/json"})
@Consumes({"application/json"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@PipelineServiceAuth
@Slf4j
public class PMSLandingDashboardResource {
  private final PMSLandingDashboardService pmsLandingDashboardService;

  @GET
  @Path("/pipelinesCount")
  @ApiOperation(value = "Get pipelines count", nickname = "getPipelinesCount")
  @NGAccessControlCheck(resourceType = "ACCOUNT", permission = "core_account_view")
  public ResponseDTO<PipelinesCount> getPipelinesCount(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam("orgProjectIdentifiers") List<OrgProjectIdentifier> orgProjectIdentifiers,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    return ResponseDTO.newResponse(pmsLandingDashboardService.getPipelinesCount(
        accountIdentifier, orgProjectIdentifiers, startInterval, endInterval));
  }
}
