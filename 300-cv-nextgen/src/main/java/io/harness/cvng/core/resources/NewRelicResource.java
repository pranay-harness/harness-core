package io.harness.cvng.core.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.newrelic.NewRelicApplication;
import io.harness.cvng.core.services.api.NewRelicService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("newrelic")
@Path("/newrelic")
@Produces("application/json")
@NextGenManagerAuth
@ExposeInternalException
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class NewRelicResource {
  @Inject private NewRelicService newRelicService;

  @GET
  @Path("/endpoints")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get all newrelic endpoints", nickname = "getNewRelicEndPoints")
  public ResponseDTO<List<String>> getNewRelicEndPoints() {
    return ResponseDTO.newResponse(newRelicService.getNewRelicEndpoints());
  }

  @GET
  @Path("/applications")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get all newrelic applications", nickname = "getNewRelicApplications")
  public ResponseDTO<List<NewRelicApplication>> getNewRelicApplications(
      @NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("connectorIdentifier") final String connectorIdentifier,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier, @QueryParam("pageSize") @NotNull int pageSize,
      @QueryParam("offset") @NotNull int offset, @QueryParam("filter") String filter,
      @QueryParam("tracingId") String tracingId) {
    return ResponseDTO.newResponse(newRelicService.getNewRelicApplications(
        accountId, connectorIdentifier, orgIdentifier, projectIdentifier, filter, tracingId));
  }
}
