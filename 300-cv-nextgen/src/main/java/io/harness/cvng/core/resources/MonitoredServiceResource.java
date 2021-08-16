package io.harness.cvng.core.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.HealthMonitoringFlagResponse;
import io.harness.cvng.core.beans.ProjectParams;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceListItemDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceResponse;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponse;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
@OwnedBy(HarnessTeam.CV)
public class MonitoredServiceResource {
  @Inject MonitoredServiceService monitoredServiceService;

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "saves monitored service data", nickname = "saveMonitoredService")
  public RestResponse<MonitoredServiceResponse> saveMonitoredService(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @NotNull @Valid @Body MonitoredServiceDTO monitoredServiceDTO) {
    return new RestResponse<>(monitoredServiceService.create(accountId, monitoredServiceDTO));
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/create-default")
  @ApiOperation(value = "created default monitored service", nickname = "createDefaultMonitoredService")
  public RestResponse<MonitoredServiceResponse> createDefaultMonitoredService(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("environmentIdentifier") String environmentIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("serviceIdentifier") String serviceIdentifier) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return new RestResponse<>(
        monitoredServiceService.createDefault(projectParams, serviceIdentifier, environmentIdentifier));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @Path("{identifier}")
  @ApiOperation(value = "updates monitored service data", nickname = "updateMonitoredService")
  public RestResponse<MonitoredServiceResponse> updateMonitoredService(
      @ApiParam(required = true) @NotNull @PathParam("identifier") String identifier,
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @NotNull @Valid @Body MonitoredServiceDTO monitoredServiceDTO) {
    Preconditions.checkArgument(identifier.equals(monitoredServiceDTO.getIdentifier()),
        String.format(
            "Identifier %s does not match with path identifier %s", monitoredServiceDTO.getIdentifier(), identifier));
    return new RestResponse<>(monitoredServiceService.update(accountId, monitoredServiceDTO));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @Path("{identifier}/health-monitoring-flag")
  @ApiOperation(value = "updates monitored service data", nickname = "setHealthMonitoringFlag")
  public RestResponse<HealthMonitoringFlagResponse> setHealthMonitoringFlag(
      @NotNull @PathParam("identifier") String identifier, @NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("enable") Boolean enable) {
    return new RestResponse<>(monitoredServiceService.setHealthMonitoringFlag(
        accountId, orgIdentifier, projectIdentifier, identifier, enable));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "list monitored service data ", nickname = "listMonitoredService")
  public ResponseDTO<PageResponse<MonitoredServiceListItemDTO>> list(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier, @QueryParam("offset") @NotNull Integer offset,
      @QueryParam("pageSize") @NotNull Integer pageSize, @QueryParam("filter") String filter) {
    return ResponseDTO.newResponse(monitoredServiceService.list(
        accountId, orgIdentifier, projectIdentifier, environmentIdentifier, offset, pageSize, filter));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("{identifier}")
  @ApiOperation(value = "get monitored service data ", nickname = "getMonitoredService")
  public ResponseDTO<MonitoredServiceResponse> get(@NotNull @PathParam("identifier") String identifier,
      @NotNull @QueryParam("accountId") String accountId, @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier) {
    return ResponseDTO.newResponse(
        monitoredServiceService.get(accountId, orgIdentifier, projectIdentifier, identifier));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/service-environment")
  @ApiOperation(value = "get monitored service data from service and env ref",
      nickname = "getMonitoredServiceFromServiceAndEnvironment")
  public ResponseDTO<MonitoredServiceResponse>
  getMonitoredServiceFromServiceAndEnvironment(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("serviceIdentifier") String serviceIdentifier,
      @NotNull @QueryParam("environmentIdentifier") String environmentIdentifier) {
    return ResponseDTO.newResponse(monitoredServiceService.get(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier, environmentIdentifier));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @Path("{identifier}")
  @ApiOperation(value = "delete monitored service data ", nickname = "deleteMonitoredService")
  public RestResponse<Boolean> delete(@ApiParam(required = true) @NotNull @PathParam("identifier") String identifier,
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") String projectIdentifier) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return new RestResponse<>(monitoredServiceService.delete(projectParams, identifier));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/environments")
  @ApiOperation(
      value = "get monitored service list environments data ", nickname = "getMonitoredServiceListEnvironments")
  public ResponseDTO<List<EnvironmentResponse>>
  getEnvironments(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier) {
    return ResponseDTO.newResponse(
        monitoredServiceService.listEnvironments(accountId, orgIdentifier, projectIdentifier));
  }
}
