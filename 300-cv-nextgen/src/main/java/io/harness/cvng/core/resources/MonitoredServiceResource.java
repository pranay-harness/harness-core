package io.harness.cvng.core.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api("monitored-service")
@Path("monitored-service")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@OwnedBy(HarnessTeam.CV)
public class MonitoredServiceResource {
  @Inject MonitoredServiceService monitoredServiceService;

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "saves monitored service data", nickname = "saveMonitoredService")
  public RestResponse<Void> saveMonitoredService(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @Valid @Body MonitoredServiceDTO monitoredServiceDTO) {
    monitoredServiceService.create(accountId, monitoredServiceDTO);
    return new RestResponse<>(null);
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("{identifier}")
  @ApiOperation(value = "get monitored service data ", nickname = "getMonitoredService")
  public ResponseDTO<MonitoredServiceDTO> get(@NotNull @PathParam("identifier") String identifier,
      @NotNull @QueryParam("accountId") String accountId, @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier) {
    return ResponseDTO.newResponse(
        monitoredServiceService.get(accountId, orgIdentifier, projectIdentifier, identifier));
  }
}
