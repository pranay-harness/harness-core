package io.harness.cvng.core.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.ChangeEventDashboardDTO;
import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.services.api.ChangeEventService;
import io.harness.cvng.core.types.ChangeCategory;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api("change-event")
@Path("change-event")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
@OwnedBy(HarnessTeam.CV)
public class ChangeEventResource {
  @Inject ChangeEventService changeEventService;

  @POST
  @Timed
  @ExceptionMetered
  @Path("register")
  @ApiOperation(value = "register a ChangeEvent", nickname = "registerChangeEvent")
  public RestResponse<Boolean> register(@ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @NotNull @Valid @Body ChangeEventDTO changeEventDTO) {
    return new RestResponse<>(changeEventService.register(accountId, changeEventDTO));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get ChangeEvent List", nickname = "getChangeEventList")
  public RestResponse<List<ChangeEventDTO>> get(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("serviceIdentifier") String serviceIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("environmentIdentifier") String environmentIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("startTs") long startTime,
      @ApiParam(required = true) @NotNull @QueryParam("endTs") long endTime,
      @ApiParam @NotNull @QueryParam("changeCategories") List<ChangeCategory> changeCategories) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(accountId)
                                                            .orgIdentifier(orgIdentifier)
                                                            .projectIdentifier(projectIdentifier)
                                                            .serviceIdentifier(serviceIdentifier)
                                                            .environmentIdentifier(environmentIdentifier)
                                                            .build();
    return new RestResponse<>(changeEventService.get(serviceEnvironmentParams, startTime, endTime, changeCategories));
  }

  @GET
  @Timed
  @Path("dashboard")
  @ExceptionMetered
  @ApiOperation(value = "get ChangeEvent dashoboard", nickname = "getChangeEventDashboard")
  public RestResponse<ChangeEventDashboardDTO> get(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("serviceIdentifier") String serviceIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("environmentIdentifier") String environmentIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("startTs") long startTime,
      @ApiParam(required = true) @NotNull @QueryParam("endTs") long endTime) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(accountId)
                                                            .orgIdentifier(orgIdentifier)
                                                            .projectIdentifier(projectIdentifier)
                                                            .serviceIdentifier(serviceIdentifier)
                                                            .environmentIdentifier(environmentIdentifier)
                                                            .build();
    return new RestResponse<>(changeEventService.getChangeEventDashboard(serviceEnvironmentParams, startTime, endTime));
  }
}
