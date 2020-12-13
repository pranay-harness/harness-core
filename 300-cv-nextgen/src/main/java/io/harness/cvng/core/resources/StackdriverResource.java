package io.harness.cvng.core.resources;

import io.harness.cvng.core.beans.stackdriver.StackdriverDashboardDTO;
import io.harness.cvng.core.services.api.StackdriverService;
import io.harness.ng.beans.PageResponse;
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

@Api("stackdriver")
@Path("/stackdriver")
@Produces("application/json")
@NextGenManagerAuth
public class StackdriverResource {
  @Inject private StackdriverService stackdriverService;

  @GET
  @Path("/dashboards")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get all stackdriver dashboards", nickname = "getStackdriverDashboards")
  public RestResponse<PageResponse<StackdriverDashboardDTO>> getStackdriverDashboards(
      @NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("connectorIdentifier") final String connectorIdentifier,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier, @QueryParam("pageSize") @NotNull int pageSize,
      @QueryParam("offset") @NotNull int offset, @QueryParam("filter") String filter) {
    return new RestResponse<>(stackdriverService.listDashboards(
        accountId, connectorIdentifier, orgIdentifier, projectIdentifier, pageSize, offset, filter));
  }
}
