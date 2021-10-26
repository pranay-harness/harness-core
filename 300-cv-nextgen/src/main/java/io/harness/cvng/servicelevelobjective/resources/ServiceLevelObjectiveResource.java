package io.harness.cvng.servicelevelobjective.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveFilter;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveResponse;
import io.harness.cvng.servicelevelobjective.services.ServiceLevelObjectiveService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.apache.commons.collections.CollectionUtils;
import retrofit2.http.Body;

@Api("slo")
@Path("slo")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
@OwnedBy(HarnessTeam.CV)
public class ServiceLevelObjectiveResource {
  @Inject ServiceLevelObjectiveService serviceLevelObjectiveService;

  @POST
  @Timed
  @ExceptionMetered
  @Path("/create")
  @ApiOperation(value = "saves slo data", nickname = "saveSLOData")
  public RestResponse<ServiceLevelObjectiveResponse> saveSLOData(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @Valid @Body ServiceLevelObjectiveDTO serviceLevelObjectiveDTO) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return new RestResponse<>(serviceLevelObjectiveService.create(projectParams, serviceLevelObjectiveDTO));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @Path("{identifier}")
  @ApiOperation(value = "update slo data", nickname = "updateSLOData")
  public RestResponse<ServiceLevelObjectiveResponse> updateSLOData(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @ApiParam(required = true) @NotNull @PathParam("identifier") String identifier,
      @NotNull @Valid @Body ServiceLevelObjectiveDTO serviceLevelObjectiveDTO) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return new RestResponse<>(serviceLevelObjectiveService.update(projectParams, identifier, serviceLevelObjectiveDTO));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @Path("{identifier}")
  @ApiOperation(value = "delete slo data", nickname = "deleteSLOData")
  public RestResponse<Boolean> deleteSLOData(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @ApiParam(required = true) @NotNull @PathParam("identifier") String identifier,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") String projectIdentifier) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return new RestResponse<>(serviceLevelObjectiveService.delete(projectParams, identifier));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get all service level objectives ", nickname = "getServiceLevelObjectives")
  public ResponseDTO<PageResponse<ServiceLevelObjectiveResponse>> getServiceLevelObjectives(
      @NotNull @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier, @QueryParam("offset") @NotNull Integer offset,
      @QueryParam("pageSize") @NotNull Integer pageSize, @QueryParam("userJourneys") List<String> userJourneys) {
    ServiceLevelObjectiveFilter serviceLevelObjectiveFilter = ServiceLevelObjectiveFilter.builder().build();
    if (CollectionUtils.isNotEmpty(userJourneys)) {
      serviceLevelObjectiveFilter.setUserJourneys(userJourneys);
    }
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return ResponseDTO.newResponse(
        serviceLevelObjectiveService.get(projectParams, offset, pageSize, serviceLevelObjectiveFilter));
  }
}
