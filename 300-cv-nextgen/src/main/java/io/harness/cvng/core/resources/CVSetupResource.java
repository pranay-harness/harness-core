package io.harness.cvng.core.resources;

import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;

import io.harness.cvng.core.beans.CVSetupStatusDTO;
import io.harness.cvng.core.services.api.CVSetupService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;

@Api("/setup")
@Path("/setup")
@Produces("application/json")
@NextGenManagerAuth
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class CVSetupResource {
  private CVSetupService cvSetupService;

  @GET
  @Path("/status")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get the status of CV related resources setup", nickname = "getCVSetupStatus")
  public RestResponse<CVSetupStatusDTO> getCVSetupStatus(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam(ORG_KEY) @NotNull String orgIdentifier, @QueryParam(PROJECT_KEY) @NotNull String projectIdentifier) {
    return new RestResponse<>(cvSetupService.getSetupStatus(accountId, orgIdentifier, projectIdentifier));
  }
}
